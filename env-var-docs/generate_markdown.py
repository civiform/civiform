"""Generates markdown from an environment variable documentation file and
writes it to a github repository.

Requires the following variables to be present in the environment:
    ENV_VAR_DOCS_PATH: the path to env-var-docs.json.

If LOCAL_OUTPUT equals "true", markdown will be written to stdout instead of GitHub.

If LOCAL_OUTPUT does not equal "true", the following variables must be set:
    RELEASE_VERSION: CiviForm version being released. 
    GITHUB_ACCESS_TOKEN: personal access token to call github API with.
    TARGET_REPO: the target github repository in 'owner/repo-name' format.
    TARGET_PATH: the relative path within TARGET_REPO to write the file to.
"""

import dataclasses
import env_var_docs.parser
import env_var_docs.errors_formatter
import github
import os
import sys
import typing


def errorexit(msg):
    """Logs a message and exits"""
    sys.stderr.write(msg + "\n")
    exit(1)


@dataclasses.dataclass
class Config:
    docs_path: str
    version: str
    local_output: bool
    access_token: str
    repo: str
    repo_path: str


def make_config() -> Config:
    """Returns a Config by reading variables set in the environment. See docs
    at the beginning of this file for required and optional variables.

    Exits if any there are any validation errors.
    """
    if "ENV_VAR_DOCS_PATH" not in os.environ:
        errorexit(
            "ENV_VAR_DOCS_PATH must be present in the environment variables")

    docs = os.environ["ENV_VAR_DOCS_PATH"]
    if not os.path.isfile(docs):
        errorexit(f"'{docs}' does not point to a file")

    local = (os.environ.get("LOCAL_OUTPUT", "false") == "true")
    if local:
        version = token = repo = path = ""
    else:
        try:
            version = os.environ["RELEASE_VERSION"]
            token = os.environ["GITHUB_ACCESS_TOKEN"]
            repo = os.environ["TARGET_REPO"]
            path = os.environ["TARGET_PATH"]
            if path[0:1] == "/":
                errorexit(
                    "f{path} must be a relative path starting from the repository root."
                )
        except KeyError as e:
            errorexit(
                f"Either LOCAL_OUTPUT=true must be set or {e.args[0]} must be present in the environment variables"
            )

    return Config(docs, version, local, token, repo, path)


def main():
    config = make_config()
    with open(config.docs_path) as docs_file:
        markdown, parse_errors = generate_markdown(docs_file)
        if len(parse_errors) != 0:
            msg = f"{config.docs_path} is invalid:\n"
            msg += env_var_docs.errors_formatter.format(parse_errors)
            errorexit(msg)

    if config.local_output:
        print(markdown)
        exit()

    gh = github.Github(config.access_token)
    repo = gh.get_repo(config.repo)

    # If file exists, update it, if not, create it.
    path = f"{config.repo_path}/{config.version}.md"
    msg = f"Adds server environment variable documentation for {config.version}"
    try:
        file = repo.get_contents(path)
        if isinstance(file, list):
            errorexit(
                f"{file_path} returns multiple files in the repo, aborting")

        res = repo.update_file(path, msg, markdown, file.sha)
    except github.GithubException.UnknownObjectException:
        res = repo.create_file(path, msg, markdown, branch="main")

    print(
        f"https://github.com/blob/main/{config.repo}/{path} updated in commit {res['commit']}"
    )


def generate_markdown(
    docs_file: typing.TextIO
) -> tuple[str | None, list[env_var_docs.parser.NodeParseError]]:
    out = ""

    def append_node_to_out(node: env_var_docs.parser.Node):
        nonlocal out  # Need to declare out nonlocal otherwise it gets shadowed.

        group = None
        if isinstance(node.details, env_var_docs.parser.Group):
            group = node.details
        var = None
        if isinstance(node.details, env_var_docs.parser.Variable):
            var = node.details

        # Write title.
        out += f"{'#' * node.level} {node.name}\n\n"

        # Write description.
        desc = ""
        if group is not None:
            desc = group.group_description
        if var is not None:
            desc = var.description
            if var.required:
                desc += " **Required**."
        out += (desc + "\n\n")

        # Write Variable validation rules.
        if var is not None:
            out += f"- Type: {var.type}\n"
            if var.values is not None:
                out += "- Allowed values:\n"
                for val in var.values:
                    out += f"   - {val}\n"
            if var.regex is not None:
                out += f"- Validation regular expression: `{var.regex}`\n"
            if var.regex_tests is not None:
                out += "- Regular expression examples:\n"
                for test in var.regex_tests:
                    msg = "should match" if test.should_match else "should not match"
                    out += f"   - `{test.val}` {msg}.\n"
            out += "\n"

    errors = env_var_docs.parser.visit(docs_file, append_node_to_out)
    if len(errors) != 0:
        return None, errors

    return out, []


if __name__ == "__main__":
    main()
