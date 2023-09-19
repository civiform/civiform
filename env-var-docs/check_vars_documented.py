"""Checks that every environment variable referenced in application.conf has documentation.

Requires the following variables to be present in the environment:
    APPLICATION_CONF_PATH: the path to application.conf.
    ENV_VAR_DOCS_PATH: the path to env-var-docs.json.

See ./README.md for instuctions on how to run this script locally.
"""

import dataclasses
import env_var_docs.parser
import env_var_docs.errors_formatter
import os
import pprint
import re
import sys
import typing
# Needed for <3.10
from typing import Union
# Needed for <3.9
from typing import Dict, Tuple, List


def errorexit(msg):
    """Logs a message and exits"""
    sys.stderr.write(msg + "\n")
    exit(1)


@dataclasses.dataclass
class Config:
    """Holds file paths read at runtime."""
    app_conf_path: str
    docs_path: str


@dataclasses.dataclass
class ServerVar:
    """Holds data about a variable found in .conf files"""
    name: str
    hocon_name: str
    file_path: str


def make_config() -> Config:
    """Returns a Config by reading the paths set in APPLICATION_CONF_PATH and
    ENV_VAR_DOCS_PATH environment variables.

    Exits if there are any validation errors.
    """

    def validate_path(env_var) -> str:
        path = os.environ[env_var]
        if not os.path.isfile(path):
            errorexit(f"'{path}' does not point to a file")
        return path

    try:
        app_conf = validate_path("APPLICATION_CONF_PATH")
        var_docs = validate_path("ENV_VAR_DOCS_PATH")
    except KeyError as e:
        errorexit(f"{e.args[0]} must be present in the environment variables")

    return Config(app_conf, var_docs)


def main():
    config = make_config()
    server_vars: Dict[str, ServerVar] = vars_from_application_conf(
        config.app_conf_path)
    with open(config.docs_path) as f:
        documented_vars, parse_errors = vars_from_docs(f)
        if len(parse_errors) != 0:
            msg = f"{config.docs_path} is invalid:\n"
            msg += env_var_docs.errors_formatter.format(parse_errors)
            errorexit(msg)

    errors = False
    msg = "CiviForm environment variables are not correctly documented. See https://github.com/civiform/civiform/blob/main/env-var-docs/README.md for information. Issues:\n"

    for name, var in documented_vars.items():
        if var.mode == env_var_docs.parser.Mode.ADMIN_READABLE:
            try:
                server_var = server_vars[name]
                if name.lower() != server_var.hocon_name:
                    errors = True
                    msg += f"\nAdmin-accessible vars must have a HOCON name matching the lower case env var name:\n\tVar name:   {server_var.name}\n\tHOCON name: {server_var.hocon_name}\n\tFile:       {server_var.file_path}\n"

            except KeyError:
                errors = True
                msg += f"\nAdmin-accessible vars must have a HOCON name matching the lower case env var name:\n\tVar name:   {name}"

    documented_var_names = set(documented_vars.keys())
    server_var_names = set([var.name for var in server_vars.values()])
    undocumented = server_var_names - documented_var_names
    overdocumented = documented_var_names - server_var_names

    if len(undocumented) != 0:
        errors = True
        msg += f"\nThe following vars are not documented in {config.docs_path}:\n{pprint.pformat(undocumented)}\n"
    if len(overdocumented) != 0:
        errors = True
        msg += f"\nThe following vars are documented in {config.docs_path} but not referenced in {config.app_conf_path}:\n{pprint.pformat(overdocumented)}"
    if errors:
        errorexit(msg)


def vars_from_application_conf(app_conf_path: str) -> Dict[str, ServerVar]:
    """Parses an application.conf file and returns the set of referenced environment variables.

    If the application.conf file includes other conf files, those files will
    also be parsed for referenced environment variables.
    """

    # Matches any comment lines.
    comment_regex = re.compile(r"^\s*(#|//)")

    # Matches any include statements. Captures the file name in group 1.
    include_regex = re.compile(r"^\s*include\s+\"(.+)\"$")

    # Matches any UPPER_SNAKE_CASE variables in substitutions like
    # ${?CIVIC_ENTITY_SMALL_LOGO_URL}. Captures the variable name in group 1.
    env_var_regex = re.compile(r"^.*\${\?\s*([A-Z0-9_]+)\s*}")

    dir_base = os.path.dirname(app_conf_path)
    files_to_parse = [app_conf_path]
    vars = dict()

    while len(files_to_parse) != 0:
        path = files_to_parse.pop()
        with open(path) as f:
            f.seek(0)  # Ensure we are reading from the start of the file.
            for line in f:
                if comment_regex.match(line) != None:
                    continue

                include_match = include_regex.match(line)
                if include_match != None:
                    assert include_match is not None  # Appease mypy.
                    include_path = include_match.group(1)
                    if include_path == None:
                        errorexit(
                            f"ERROR: include_regex matched on {line} but found no file path"
                        )

                    files_to_parse.append(os.path.join(dir_base, include_path))
                    continue

                match = env_var_regex.match(line)
                if match == None:
                    continue
                assert match is not None  # Appease mypy.
                var = match.group(1)
                if var == None:
                    continue

                vars[var] = ServerVar(var, line.split("=")[0].strip(), path)

    return vars


def vars_from_docs(
    docs_file: typing.TextIO
) -> Tuple[Union[Dict[str, env_var_docs.parser.Variable], None],
           List[env_var_docs.parser.NodeParseError]]:
    """If docs_file has no parsing errors, returns the set of defined
    environment variables in an environment variable documentation file.
    Otherwise returns the parsing errors.
    """
    vars = dict()

    def add(node: env_var_docs.parser.Node):
        if isinstance(node.details, env_var_docs.parser.Variable):
            vars[node.name] = node.details

    errors = env_var_docs.parser.visit(docs_file, add)
    if len(errors) != 0:
        return None, errors

    return vars, []


if __name__ == "__main__":
    main()
