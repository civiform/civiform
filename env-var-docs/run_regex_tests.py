"""Checks that every regex test in env-var-docs.json passes.

Requires the following variables to be present in the environment:
    ENV_VAR_DOCS_PATH: the path to env-var-docs.json.

See ./README.md for instuctions on how to run this script locally.
"""

import dataclasses
import env_var_docs.parser
import env_var_docs.errors_formatter
import os
import re
import sys
import typing
# Needed for <3.10
from typing import Union
# Needed for <3.9
from typing import Tuple, List


def errorexit(msg):
    """Logs a message and exits"""
    sys.stderr.write(msg + "\n")
    exit(1)


def env_var_docs_path() -> str:
    """Returns the path to the env-var-docs.json file set by the
    ENV_VAR_DOCS_PATH environment variable.

    Exits if there are any validation errors.
    """
    try:
        path = os.environ["ENV_VAR_DOCS_PATH"]
        if not os.path.isfile(path):
            errorexit(f"'{path}' does not point to a file")
    except KeyError as e:
        errorexit(f"{e.args[0]} must be present in the environment variables")

    return path


def main():
    docs_path = env_var_docs_path()
    with open(docs_path) as f:
        failed_tests, parse_errors = run_tests(f)
        if len(parse_errors) != 0:
            msg = f"{docs_path} is invalid:\n"
            msg += env_var_docs.errors_formatter.format(parse_errors)
            errorexit(msg)

    if len(failed_tests) != 0:
        errorexit(f"Test failures: {failed_tests}")


def run_tests(
    docs_file: typing.TextIO
) -> Tuple[Union[List[str], None], List[env_var_docs.parser.NodeParseError]]:
    """Runs any provided regex tests in an environment variable documentation file.

    Returns a list of environment variable names that failed their tests. Test
    failure or pass messages are printed as the tests are run.
    """

    failures = []

    def run_test(node: env_var_docs.parser.Node):
        if not isinstance(node.details, env_var_docs.parser.Variable):
            return

        var = node.details
        if var.regex is None:
            return

        # appease mypy.
        assert var.regex is not None
        assert var.regex_tests is not None
        result = _run_test(node.name, var.regex, var.regex_tests)
        print(result)
        if result.regex_error != "" or result.failing_matches:
            failures.append(node.name)

    parse_errors = env_var_docs.parser.visit(docs_file, run_test)
    if len(parse_errors) != 0:
        return None, parse_errors

    return failures, []


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
    matches: List[Match]
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


def _run_test(
        node_name: str, regex_text: str,
        tests: List[env_var_docs.parser.RegexTest]) -> Test:
    try:
        regex = re.compile(regex_text)
    except re.error as e:
        # Test failed because the regex could not compile.
        return Test(
            variable=node_name,
            regex=regex_text,
            regex_error=e.msg,
            matches=[],
            failing_matches=False)

    matches = []
    failing_matches = False
    for test in tests:
        did_match = (regex.match(test.val) != None)
        if test.should_match != did_match:
            failing_matches = True
        matches.append(Match(test.val, test.should_match, did_match))

    return Test(
        variable=node_name,
        regex=regex_text,
        regex_error="",
        matches=matches,
        failing_matches=failing_matches)


if __name__ == "__main__":
    main()
