#!/usr/bin/env python3

import dataclasses
from jinja2 import Environment, PackageLoader, select_autoescape
import env_var_docs.parser
import env_var_docs.errors_formatter
import typing
import os
import sys

ParsedGroup = typing.TypeVar("ParsedGroup")

@dataclasses.dataclass
class ParsedGroup:
    group_name: str
    group_description: str
    sub_groups: list[ParsedGroup] = dataclasses.field(default_factory=list)
    variables: dict[str, env_var_docs.parser.Variable] = dataclasses.field(default_factory=dict)


@dataclasses.dataclass
class Config:
    docs_path: str
    local_output: bool


def error_exit(msg):
    """Logs a message and exits"""
    sys.stderr.write(msg + "\n")
    exit(1)


def make_config() -> Config:
    if "ENV_VAR_DOCS_PATH" not in os.environ:
        error_exit(
            "ENV_VAR_DOCS_PATH must be present in the environment variables")

    docs = os.environ["ENV_VAR_DOCS_PATH"]
    if not os.path.isfile(docs):
        error_exit(f"'{docs}' does not point to a file")

    local = (os.environ.get("LOCAL_OUTPUT", "false") == "true")

    return Config(docs, local)


def main():
    config = make_config()
    with open(config.docs_path) as docs_file:
        manifest, parse_errors = generate_manifest(docs_file)

        if len(parse_errors) != 0:
            msg = f"{config.docs_path} is invalid:\n"
            msg += env_var_docs.errors_formatter.format(parse_errors)
            error_exit(msg)

    if config.local_output:
        print(manifest)
        exit()

    with open("server/app/services/settings/SettingsManifest.java", "w", encoding="utf-8") as manifest_file:
        manifest_file.write(manifest)


def generate_manifest(docs_file: typing.TextIO) -> tuple[str | None, list[env_var_docs.parser.NodeParseError]]:
    root_group = ParsedGroup("ROOT", "ROOT")
    docs: dict[str, ParsedGroup | Variable] = { "file": root_group }

    def visitor(node: env_var_docs.parser.Node):
        nonlocal docs

        if isinstance(node.details, env_var_docs.parser.Group):
            group = ParsedGroup(node.name, node.details.group_description)
            parent = docs[node.json_path]
            parent.sub_groups.append(group)
            docs[node.json_path + "." + node.name] = group
        else:
            parent = docs[node.json_path]
            parent.variables[node.name] = node.details

    errors = env_var_docs.parser.visit(docs_file, visitor)
    print(docs)

    if len(errors) != 0:
        return None, errors

    env = Environment(
        loader=PackageLoader("settings_manifest"),
        autoescape=select_autoescape()
    )

    template = env.get_template("SettingsManifest.java.jinja")

    return template.render(honk="woohoo"), []


if __name__ == "__main__":
    main()
