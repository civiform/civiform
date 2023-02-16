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
import env_var_docs.validator
import env_var_docs.visitor
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
            if path[0:1] == "/":
                errorexit("f{path} must be a relative path")
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


def generate_markdown(docs_file: typing.TextIO) -> str:
    out = ""

    def output(node: env_var_docs.visitor.NodeInfo):
        nonlocal out  # Need to declare out nonlocal otherwise it gets shadowed.
        out += f"{'#' * node.level} {node.name}\n\n"

        nd = node.details
        desc = ""
        if node.type == "group":
            desc = nd["group-description"]
        else:
            desc = nd["description"]
            if "required" in nd and nd["required"]:
                desc += " **Required**."
        out += (desc + "\n\n")

        if node.type == "variable":
            if "type" in nd:
                out += f"- Type: {nd['type']}\n"
            if "values" in nd:
                out += "- Allowed values:\n"
                for val in nd["values"]:
                    out += f"   - {val}\n"
            if "regex" in nd:
                out += f"- Validation regular expression: `{nd['regex']}`\n"
            if "regex_tests" in nd:
                out += "- Regular expression examples:\n"
                for test in nd["regex_tests"]:
                    msg = "should match" if test[
                        "shouldMatch"] else "should not match"
                    out += f"   - `{test['val']}` {msg}.\n"
            out += "\n"

    env_var_docs.visitor.visit(docs_file, output)
    return out


if __name__ == "__main__":
    main()
