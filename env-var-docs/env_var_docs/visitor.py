"""Provides functionality for visiting each node in an environment variable documentation file.
"""

import dataclasses
import typing
import json


@dataclasses.dataclass
class NodeInfo:
    """A node within an environment variable documentation file.

    See schema/schema.json for a specification of the types of nodes and their
    fields.
    """

    level: int
    """Level of the node within the file.
    
    A variable documentation file has an implicit root node, so top-level nodes
    within the file have level 1.
    """

    type: str
    """Either 'group' or 'variable'."""

    name: str
    """The group name or variable name."""

    details: dict[str, typing.Any]
    """The group details or variable details."""


VisitFn = typing.Callable[[NodeInfo], None]


def visit(file: typing.TextIO, visit_fn: VisitFn):
    """Parses an env-var-docs.json file and calls visit_fn on each node.

	The file is walked preorder (visit root then children). This function
	assumes the file passes json-schema validation accoriding to the schema
	specified in env-var-docs/schema.json.
    """
    file.seek(0)  # Ensure we are reading from the beginning of the file.

    # The file is an implicit root node. Recursively descend into each top-level field.
    for key, value in json.load(file).items():
        _visit(1, key, value, visit_fn)


def _visit(
        level: int, name: str, details: dict[str, typing.Any],
        visit_fn: VisitFn):
    """Recursively visits a node and its children, in preorder traversal order (visit root then children)."""

    if "group-description" in details:
        visit_fn(NodeInfo(level, "group", name, details))
        for sub_name, sub_details in details["members"].items():
            _visit(level + 1, sub_name, sub_details, visit_fn)
    else:
        visit_fn(NodeInfo(level, "variable", name, details))
