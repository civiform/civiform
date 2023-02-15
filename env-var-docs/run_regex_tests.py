"""Checks that every regex test in env-var-docs.json passes.

Requires the following variables to be present in the environment:
    ENV_VAR_DOCS_PATH: the path to env-var-docs.json.
"""

import dataclasses
import env_var_docs.validator
import env_var_docs.visitor
import os
import re
import sys
import typing


def errorexit(msg):
    """Logs a message and exits"""
    sys.stderr.write(msg + "\n")
    exit(1)


def validate_env_variables() -> str:
    """Parses expected environment variables and returns the path to the env-var-docs.json file.

    Exits if any there are any validation errors.
    """
    try:
        path = os.environ["ENV_VAR_DOCS_PATH"]
        if not os.path.isfile(path):
            errorexit(f"'{path}' does not point to a file")
    except KeyError as e:
        errorexit(f"{e.args[0]} must be present in the environment variables")

    return path


def main():
    docs_path = validate_env_variables()
    with open(docs_path) as f:
        err = env_var_docs.validator.validate(f)
        if err != "":
            errorexit(f"{docs_path} is not valid: {err}")
        results = run_tests(f)

    if len(results) != 0:
        errorexit(f"Test failures: {results}")


def run_tests(docs_file: typing.TextIO) -> list[str]:
    """Runs any provided regex tests in an environment variable documentation file.

    Returns a list of environment variable names that failed their tests.
    """

    failures = []

    def run_test(node: env_var_docs.visitor.NodeInfo):
        if node.type != "variable":
            return
        if "regex" not in node.details:
            return

        result = _run_test(node)
        print(result)
        if result.regex_error != "" or result.failing_matches:
            failures.append(node.name)

    env_var_docs.visitor.visit(docs_file, run_test)
    return failures


@dataclasses.dataclass
class Match:
    input: str
    shouldMatch: bool
    didMatch: bool


@dataclasses.dataclass
class Test:
    variable: str
    regex: str
    regex_error: str
    matches: list[Match]
    failing_matches: bool

    def __str__(self) -> str:
        """Formats a Test for printing."""
        status = "PASSED"
        if self.regex_error != "":
            status = "FAILED: invalid regex"
        if self.failing_matches:
            status = "FAILED: not all matches passed"

        out = f"RUN '{self.variable}' {status}\n"
        if status == "PASSED":
            return out

        out += f"\tRegex: '{self.regex}'\n"
        if self.regex_error != "":
            out += f"\tRegex compile error: {self.regex_error}\n"
            return out  # If regex did not compile, there will be no failed matches.

        for match in self.matches:
            out += (
                f"\tRUN '{match.input}' {'PASSED:' if match.shouldMatch == match.didMatch else 'FAILED:'}\n"
                f"\t\tShould match: {match.shouldMatch}\n"
                f"\t\tDid  match: {match.didMatch}\n")

        return out


def _run_test(node: env_var_docs.visitor.NodeInfo) -> Test:
    """Runs a test for an environment variable node.

    Throws an AssertionError if any of the following is true:
      - node.type is not "variable"
      - "regex" is not in node.details.
    """
    assert node.type == "variable"
    assert "regex" in node.details

    regex_text = node.details["regex"]
    try:
        regex = re.compile(regex_text)
    except re.error as e:
        return Test(
            variable=node.name,
            regex=regex_text,
            regex_error=e.msg,
            matches=[],
            failing_matches=False)

    tests = node.details["regex_tests"]
    matches = []
    failing_matches = False
    for test in tests:
        val = test["val"]
        shouldMatch = test["shouldMatch"]
        didMatch = (regex.match(val) != None)
        if shouldMatch != didMatch:
            failing_matches = True
        matches.append(Match(val, shouldMatch, didMatch))

    return Test(
        variable=node.name,
        regex=regex_text,
        regex_error="",
        matches=matches,
        failing_matches=failing_matches)


if __name__ == "__main__":
    main()
