import env_var_docs.parser
# Needed for <3.9
from typing import List


def format(errors: List[env_var_docs.parser.NodeParseError]) -> str:
    """Formats a list of NodeParseErrors returned from
    env_var_docs.parser.visit as a human-readable string.

    Example:

    errors = env_var_docs.parser.visit(file, fn)
    if len(errors) != 0:
        print(f"file is invalid:\n{env_var_docs.errors_formatter(errors)}")
    """
    msg = ""

    for error in errors:
        msg += f"{error.path}:\n"

        msg += "\tErrors from parsing as a Variable:\n"
        for e in error.variable_errors:
            msg += f"\t\t{e.path}: {e.msg}\n"

        msg += f"\tErrors from parsing as a Group:\n"
        for e in error.group_errors:
            msg += f"\t\t{e.path}: {e.msg}\n"

    return msg
