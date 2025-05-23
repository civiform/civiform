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

import base64
import dataclasses
import env_var_docs.errors_formatter
import env_var_docs.parser
import github
import os
import requests
import sys
import typing
# Needed for <3.10
from typing import Union
# Needed for <3.9
from typing import List, Tuple


def error_exit(msg):
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
        error_exit(
            "ENV_VAR_DOCS_PATH must be present in the environment variables")

    docs = os.environ["ENV_VAR_DOCS_PATH"]
    if not os.path.isfile(docs):
        error_exit(f"'{docs}' does not point to a file")

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
                error_exit(
                    "f{path} must be a relative path starting from the repository root."
                )
        except KeyError as e:
            error_exit(
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
            error_exit(msg)

    if config.local_output:
        print(markdown)
        exit()

    gh = github.Github(config.access_token)
    repo = gh.get_repo(config.repo)
    branch = repo.get_branch("main")
    head_oid = branch.commit.sha
    commit_message = ""

    # If file exists, update it, if not, create it.
    path = f"{config.repo_path}/{config.version}.md"
    commit_message = None
    updates = [
        {
            "path":
                path,
            "contents":
                base64.b64encode(markdown.encode("utf-8")).decode("utf-8")
        }
    ]
    try:
        file = repo.get_contents(path)
        if isinstance(file, List):
            error_exit(f"{path} returns multiple files in the repo, aborting")

        if file.decoded_content.decode() == markdown:
            print(f"{path} already exists and does not need to be updated.")
            return
        commit_message = f"Updates {config.version} server environment variable documentation"
    except github.UnknownObjectException:
        commit_message = f"Adds {config.version} server environment variable documentation"

        # Update SUMMARY.md with a link to the new file. First, get all versioned docs files in the repo.
        docs_paths = []
        res = repo.get_contents(config.repo_path)
        for f in res:
            if f.name != "README.md":
                docs_paths.append(f.path)

        docs_paths.append(path)
        summary_file = repo.get_contents("docs/SUMMARY.md")
        new_contents = new_summary(
            summary_file.decoded_content.decode(), docs_paths)

        updates.append(
            {
                "path":
                    "docs/SUMMARY.md",
                "contents":
                    base64.b64encode(new_contents.encode("utf-8")
                                    ).decode("utf-8")
            })
    # GraphQL mutation that will create a signed commit automatically
    mutation = """
    mutation($input: CreateCommitOnBranchInput!) {
        createCommitOnBranch(input: $input) {
            commit {
                oid
                commitUrl
            }
        }
    }
    """
    variables = {
        "input":
            {
                "branch":
                    {
                        "repositoryNameWithOwner": config.repo,
                        "branchName": "main"
                    },
                "expectedHeadOid": head_oid,
                "message": {
                    "headline": commit_message,
                    "body": ""
                },
                "fileChanges": {
                    "additions": updates
                }
            }
    }

    url = "https://api.github.com/graphql"
    headers = {
        "Authorization": f"Bearer {config.access_token}",
        "Accept": "application/vnd.github.v4+json"
    }
    payload = {"query": mutation, "variables": variables}
    resp = requests.post(url, json=payload, headers=headers)
    resp.raise_for_status()
    data = resp.json()
    if "errors" in data:
        raise RuntimeError(data["errors"])
    commit = data["data"]["createCommitOnBranch"]["commit"]
    print(f"{commit['commitUrl']} in commit {commit['oid']}")


def generate_markdown(
    docs_file: typing.TextIO
) -> Tuple[Union[str, None], List[env_var_docs.parser.NodeParseError]]:
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
            if var.mode == env_var_docs.parser.Mode.ADMIN_WRITEABLE:
                out += "**Admin writeable**\n\n"
            elif var.mode == env_var_docs.parser.Mode.ADMIN_READABLE:
                out += "**Admin readable**\n\n"
            elif var.mode == env_var_docs.parser.Mode.SECRET:
                out += "**Managed secret**\n\n"
            else:
                out += "**Server setting**\n\n"

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
                    out += f"   - `{val}`\n"
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


# These are added to support Python <3.9
def removesuffix(text, suffix):
    return text[:-len(suffix)] if text.endswith(suffix) else text


def removeprefix(text, prefix):
    return text[len(prefix):] if text.startswith(prefix) else text


def version_key(path: str):
    # Expected input is a path to an env-var-docs markdown, e.g.
    # docs/it-manual/sre-playbook/server-environment-variables/v2.40.0.md
    filename = path.rsplit("/", 1)[-1]
    ver = filename.lstrip("v").rstrip(".md")
    parts = ver.split(".")
    return tuple(int(p) for p in parts)


def new_summary(current_summary: str, docs_paths: List[str]) -> str:
    """In SUMMARY.md we have a '[CiviForm server environment variables]' list
    item that has sublist items for each versioned environment variable
    documentation markdown files. This function updates the sublist items to
    match docs_file_paths.
    """

    def format_docs_links(indent_level: int, docs_paths: List[str]) -> str:
        docs_paths.sort(key=version_key)
        out = ""
        for p in docs_paths:
            indent = " " * indent_level
            name = removesuffix(os.path.basename(p), ".md")
            link = removeprefix(
                p, "docs/"
            )  # gitbook root is in docs/ directory of civiform/docs repo.
            out += f"{indent}  * [{name}]({link})\n"
        return out

    out = ""
    skip_line = False
    indent_level = None
    for line in current_summary.splitlines(keepends=True):
        # Once we get to the docs parent list item:
        #
        # 1. note the indent level of the parent list item.
        # 2. add the parent list item and updated sub list items to out.
        # 3. keep looping over lines in current_summary, do not add line to out
        #    until we get to a list item that has the less than or equal to the
        #    indent level of the parent list item.
        if line.find(
                "* [CiviForm server environment variables](it-manual/sre-playbook/server-environment-variables)"
        ) != -1:
            skip_line = True
            indent_level = line.find("*")
            out += line
            out += format_docs_links(indent_level, docs_paths)
            continue

        if skip_line:
            if indent_level is not None and line.find("*") <= indent_level:
                skip_line = False
                out += line
        else:
            out += line

    return out


if __name__ == "__main__":
    main()
