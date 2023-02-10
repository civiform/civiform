"""Generates markdown from an environment variable documentation file and write it to a github repository.

Requires the following variables to be present in the environment:
    ENV_VAR_DOCS_PATH: the path to env-var-docs.json.

If LOCAL_OUTPUT is set, markdown will be written to stdout instead of GitHub.

If LOCAL_OUTPUT is not set, the following variables must be set:
    RELEASE_VERSION: CiviForm version being released. 
    GITHUB_ACCESS_TOKEN: personal access token to call github API with.
    TARGET_REPO: the target github repository in 'owner/repo-name' format.
    TARGET_PATH: the relative path within TARGET_REPO to write the file to.
"""

import dataclasses
import github
import env_var_docs.validator
import env_var_docs.visitor
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


def validate_env_variables() -> Config:
    """Parses expected environment variables and returns a config.

    Exits if any there are any validation errors.
    """
    try:
        docs = os.environ["ENV_VAR_DOCS_PATH"]
        if not os.path.isfile(docs):
            errorexit(f"'{docs}' does not point to a file")

        local = "LOCAL_OUTPUT" in os.environ
        if not local:
            version = os.environ["RELEASE_VERSION"]
            token = os.environ["GITHUB_ACCESS_TOKEN"]
            repo = os.environ["TARGET_REPO"]
            path = os.environ["TARGET_PATH"]
        else:
            version = token = repo = path = ""

    except KeyError as e:
        errorexit(f"{e.args[0]} must be present in the environment variables")

    return Config(docs, version, local, token, repo, path)


def main():
    config = validate_env_variables()
    with open(config.docs_path) as docs_file:
        err = env_var_docs.validator.validate(docs_file)
        if err != "":
            errorexit(f"{config.docs_path} is not valid: {err}")
        markdown = generate_markdown(docs_file)

    if config.local_output:
        print(markdown)
        exit()

    gh = github.Github(config.access_token)
    repo = gh.get_repo(config.repo)
    repo.create_file(
        f"{config.repo_path}/{config.version}.md",
        f"Adds server environment variable documentation for {config.version}",
        markdown,
        branch="main")


def generate_markdown(docs_file: typing.TextIO) -> str:
    out = ""
    def output(node: env_var_docs.visitor.NodeInfo):
        nonlocal out
        out += f"{'#' * node.level} {node.name}\n\n"

        if node.type == "group":
            desc = node.details["group-description"]
        else:
            desc = node.details["description"]
        out += (desc + "\n\n")

        if node.type == "variable":
            for key in ['type', 'required', 'values', 'regex']:
                if key in node.details:
                    out += f"- {key.capitalize()}: `{node.details[key]}`\n"
            out += "\n"

    env_var_docs.visitor.visit(docs_file, output)
    return out


if __name__ == "__main__":
    main()
