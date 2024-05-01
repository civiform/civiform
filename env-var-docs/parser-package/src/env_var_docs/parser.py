"""Provides a 'visit' function that parses an environment variable
documentation file and returns any parsing errors due to unexpected structure.
If no parsing errors are found, 'visit' runs a user-provided function on each
node in the file.

See parser-package/README.md for a description of the expected structure.
"""

import dataclasses
from enum import Enum
import typing
import json
# Needed for <3.10
from typing import Union
# Needed for <3.9
from typing import Dict, List, Tuple

UnparsedJSON = Dict[str, typing.Any]

Mode = Enum('Mode', ['HIDDEN', 'ADMIN_READABLE', 'ADMIN_WRITEABLE', 'SECRET'])


@dataclasses.dataclass
class Group:
    """A group of Variables.

    See parser-package/README.md for the expected JSON structure.
    """
    group_description: str
    members: Dict[str, UnparsedJSON]


@dataclasses.dataclass
class RegexTest:
    """A test case for a Variable's regex.

    See parser-package/README.md for the expected JSON structure.
    """

    val: str
    """Value to test regex on."""

    should_match: bool
    """If the regex should match val."""


@dataclasses.dataclass
class Variable:
    """An environment variable referenced in application.conf.

    See parser-package/README.md for the expected JSON structure.
    """
    description: str
    type: str
    required: bool
    values: Union[List[str], None]
    regex: Union[str, None]
    regex_tests: Union[List[RegexTest], None]
    mode: Mode


@dataclasses.dataclass
class Node:
    """A node within an environment variable documentation file.

    An environment variable documentation file is a JSON object that has
    object-typed fields. Valid object values are either a Group or Variable.
    """

    level: int
    """Level of the node within the file.
    
    The environment variable documentation file has an implicit root node, so
    top-level fields within the file have level 1.
    """

    json_path: str
    """Key path of the node. Every path starts with 'file.'"""

    name: str
    """The group name or variable name. The JSON field key is the name."""

    details: Union[Group, Variable]
    """The group details or variable details."""


VisitFn = typing.Callable[[Node], None]
"""A function that takes a Node and does something with it."""


@dataclasses.dataclass
class ParseError:
    """Invalid structures encountered while parsing the JSON structure are
    reported through a ParseError.
    """

    path: str
    """The JSON path of the invalid object."""

    msg: str
    """"Why the object is invalid."""


ParseErrors = List[ParseError]


@dataclasses.dataclass
class NodeParseError:
    """Invalid fields in the environment documentation file are reported
    through a NodeParseError. A field is invalid if it can not be successfully
    parsed as a Group or a Variable.
    """

    path: str
    """The JSON path of the invalid object."""

    group_errors: ParseErrors
    """Errors from attempting to parse as a Group."""

    variable_errors: ParseErrors
    """Errors from attempting to parse as a Variable."""


def visit(file: typing.TextIO, visit_fn: VisitFn) -> List[NodeParseError]:
    """Parses an environment variable documentation file. The file must be
    valid JSON and be a single object.

    The file is completely parsed before any visit_fn is run. If there are
    parsing errors, no visit_fn is run and the errors are returned.

    If the file has no parsing errors, visit_fn is called on each node in
    preorder traversal (visit node then its children) order.
    """
    file.seek(0)  # Ensure we are reading from the beginning of the file.

    def raise_on_duplicate_keys(fields):
        """Callback for json.load object_pairs_hook parameter."""
        out = {}
        for k, v in fields:
            if k in out:
                raise ValueError(f"found duplicate key '{k}'")
            out[k] = v
        return out

    json_root = "file"
    try:
        docs = json.load(file, object_pairs_hook=raise_on_duplicate_keys)
    except (json.JSONDecodeError, ValueError) as e:
        return [
            NodeParseError(
                json_root, [ParseError(json_root, f"file is not valid: {e}")],
                [])
        ]
    if not isinstance(docs, Dict):
        return [
            NodeParseError(
                json_root,
                [ParseError(json_root, f"file is not a single JSON object")],
                [])
        ]

    nodes_to_visit: List[Node] = []
    parsing_errors: List[NodeParseError] = []

    # The file is an implicit root node. Recursively descend into each top-level field.
    for key, value in docs.items():
        _recursively_parse(
            1, json_root, key, value, nodes_to_visit, parsing_errors)

    if len(parsing_errors) != 0:
        return parsing_errors

    for node in nodes_to_visit:
        visit_fn(node)

    return []


def _recursively_parse(
        level: int, parent_path: str, key: str, value: UnparsedJSON,
        nodes_to_visit: List[Node], parsing_errors: List[NodeParseError]):
    """Recursively visits a node and its children, in preorder traversal order
    (visit root then children).

    It is valid for value to be either a Group or a Variable. If it
    successfully parses as a Group, it is a Group, if it successfully parses as
    a Variable, it is a Variable.
    
    If value does not parse as a Group nor a Variable, it is invalid.
    """
    path = _path(parent_path, key)

    group, group_errors = _try_parse_group(path, value)
    if len(group_errors) == 0:
        # A Group was successfully parsed. Add the Group to the visit list then
        # attempt to parse each field in its members.

        assert group is not None  # appease mypy
        nodes_to_visit.append(
            Node(level=level, json_path=parent_path, name=key, details=group))

        for sub_key, sub_value in group.members.items():
            _recursively_parse(
                level + 1, path, sub_key, sub_value, nodes_to_visit,
                parsing_errors)
        return

    var, var_errors = _try_parse_variable(path, value)
    if len(var_errors) == 0:
        # A Variable was successfully parsed. Add the Variable to the visit list.

        assert var is not None  # appease mypy
        nodes_to_visit.append(
            Node(level=level, json_path=parent_path, name=key, details=var))
        return

    # If we are here, neither a Group nor a Variable was successfully parsed.
    # Append parsing errors.
    parsing_errors.append(NodeParseError(path, group_errors, var_errors))


def _try_parse_group(
        parent_path: str,
        obj: UnparsedJSON) -> Tuple[Union[Group, None], ParseErrors]:
    """Attempts to parse a Group from obj. If ParseErrors are encountered, None is returned."""
    errors = []

    # Parse the 'group_description' field.
    group_description, errs = _parse_field(
        parent_path=parent_path,
        key="group_description",
        json_type=str,
        required=True,
        obj=obj)
    errors.extend(errs)

    # Parse the 'members' field.
    members, errs = _parse_field(
        parent_path=parent_path,
        key="members",
        json_type=Dict,
        required=True,
        obj=obj)
    errors.extend(errs)

    # Check that no other fields are defined.
    errors.extend(
        _ensure_no_extra_fields(
            parent_path, obj, ["group_description", "members"]))

    if len(errors) != 0:
        return None, errors
    else:
        # appease mypy
        assert group_description is not None
        assert members is not None
        return Group(group_description, members), []


def _try_parse_variable(
        parent_path: str,
        obj: UnparsedJSON) -> Tuple[Union[Variable, None], ParseErrors]:
    """Attempts to parse a Variable from obj. If ParseErrors are encountered, None is returned."""
    errors = []

    # Parse the 'description' field.
    description, errs = _parse_field(
        parent_path=parent_path,
        key="description",
        json_type=str,
        required=True,
        obj=obj)
    errors.extend(errs)

    # Parse the 'type' field.
    type, errs = _parse_field(
        parent_path=parent_path,
        key="type",
        json_type=str,
        required=True,
        obj=obj,
        checks=[_variable_check_type_is_valid])
    errors.extend(errs)

    # Parse the 'required' field.
    required, errs = _parse_field(
        parent_path=parent_path,
        key="required",
        json_type=bool,
        required=False,
        obj=obj,
        default=False)
    errors.extend(errs)

    # Parse the 'mode' field.
    def extract_mode(obj: UnparsedJSON) -> Mode:
        return Mode[obj["mode"]]

    mode, errs = _parse_field(
        parent_path=parent_path,
        key="mode",
        json_type=str,
        required=True,
        return_type=Mode,
        extract_fn=extract_mode,
        obj=obj,
        checks=[_variable_check_mode_is_valid])
    errors.extend(errs)

    # Parse the 'values' field. Get out a list[str] instead of a JSON list.
    def convert_values(obj: UnparsedJSON) -> List[str]:
        out = []
        for v in obj["values"]:
            out.append(v)
        return out

    values, errs = _parse_field(
        parent_path=parent_path,
        key="values",
        json_type=List,
        required=False,
        obj=obj,
        checks=[
            _variable_check_values, _variable_check_regex_fields_not_defined
        ],
        return_type=List[str],
        extract_fn=convert_values)
    errors.extend(errs)

    # Parse the 'regex' field.
    regex, errs = _parse_field(
        parent_path=parent_path,
        key="regex",
        json_type=str,
        required=False,
        obj=obj,
        checks=[
            _variable_check_values_not_defined,
            _variable_check_regex_tests_defined
        ])
    errors.extend(errs)

    # Parse the 'regex_tests' field. Get out a list[RegexTest] instead of a JSON list.
    def convert_regex_tests(obj: UnparsedJSON) -> List[RegexTest]:
        out = []
        for test in obj["regex_tests"]:
            out.append(RegexTest(test["val"], test["should_match"]))
        return out

    regex_tests, errs = _parse_field(
        parent_path=parent_path,
        key="regex_tests",
        json_type=List,
        required=False,
        obj=obj,
        checks=[_variable_check_regex_defined, _variable_check_regex_tests],
        return_type=List[RegexTest],
        extract_fn=convert_regex_tests)
    errors.extend(errs)

    # Check that no other fields are defined.
    errors.extend(
        _ensure_no_extra_fields(
            parent_path, obj, [
                "description", "type", "required", "values", "regex",
                "regex_tests", "mode"
            ]))

    if len(errors) != 0:
        return None, errors
    else:
        # appease mypy
        assert description is not None
        assert type is not None
        assert required is not None
        assert mode is not None
        return Variable(
            description, type, required, values, regex, regex_tests, mode), []


CheckFn = typing.Callable[[str, UnparsedJSON], ParseErrors]
"""A CheckFn takes in a parent_path and an object and returns any ParseErrors."""

T = typing.TypeVar("T")
ExtractFn = typing.Callable[[UnparsedJSON], T]
"""An ExtractFn takes in an object and returns a complex type."""


def _parse_field(
    parent_path: str,
    key: str,
    json_type: type,
    required: bool,
    obj: UnparsedJSON,
    default: Union[T, None] = None,
    checks: List[CheckFn] = [],
    return_type: Union[typing.Type[T], None] = None,
    extract_fn: Union[ExtractFn, None] = None
) -> Tuple[Union[T, None], ParseErrors]:
    """Parses a field within obj.

    ParseErrors are returned if:
      1. The field is required and not defined.
      2. The field is defined but is not the type specified by json_type.
      3. Any checks returns ParseErrors.

    If no ParseErrors are produced, returns a value of type json_type. A more
    complex type can be returned by setting return_type and extract_fn.

    Required args:
      parent_path: Path to obj.
      key: The key within obj to parse.
      json_type: The type expected at key. Must be one of [str, bool, Dict, List] or ValueError is raised.
      required: If the field is required.
      obj: The object to parse.

    Optional args:
      default: If the field is not required and not defined, default will be returned. If required is True and default is not None, a ValueError is raised.
      checks: Custom checks that can produce additional ParseErrors. CheckFns are only run if key in obj and isinstance(obj[key], json_type).
      return_type: The type returned, can be a complex type. If set, extract_fn must not be None.
      extract_fn: A function that returns a return_type instance from obj. extract_fn is only run if:
        1. key in obj and isinstance(obj[key], json_type).
        2. No checks return any ParseErrors.
    """
    if json_type not in [str, bool, Dict, List]:
        raise ValueError(
            f"'{json_type}' is not a valid json_type, valid types are [str, bool, Dict, List]"
        )
    if required and default is not None:
        raise ValueError("'default' must be None if 'required' is True")
    if return_type is not None and extract_fn is None:
        raise ValueError(
            "'extract_fn' must be provided if 'return_type' is provided")
    if extract_fn is not None and return_type is None:
        raise ValueError(
            "'return_type' must be provided if 'extract_fn' is provided")

    value = None
    errors = []

    if key not in obj:
        if required:
            errors.append(
                ParseError(parent_path, f"'{key}' is a required field"))
        else:
            value = default
    else:
        if not isinstance(obj[key], json_type):
            # Printing json_type directly produces strings like "<class'str'>".
            # To make the error messages more readable, use these strings instead.
            if json_type == str:
                t = "string"
            if json_type == bool:
                t = "bool"
            if json_type == Dict:
                t = "object"
            if json_type == List:
                t = "list"
            errors.append(ParseError(_path(parent_path, key), f"must be a {t}"))
        else:
            for check in checks:
                errors.extend(check(parent_path, obj))
            if len(errors) == 0:
                value = obj[key] if extract_fn is None else extract_fn(obj)

    return value, errors


def _path(*args: str) -> str:
    """Creates a JSON key path from keys.

    Raises a ValueError if no called with no arguments or any of the arguments
    is the empty string.
    """
    if len(args) == 0:
        raise ValueError("must have at least one argument")
    for a in args:
        if a == "":
            raise ValueError("arguments should not be the empty string")

    return ".".join(args)


def _ensure_no_extra_fields(
        parent_path: str, obj: UnparsedJSON,
        valid_fields: List[str]) -> ParseErrors:
    """Returns ParseErrors for any keys in obj that are not in valid_fields."""
    errors = []
    for key in list(obj):
        if key not in valid_fields:
            errors.append(
                ParseError(
                    parent_path,
                    f"'{key}' is an invalid key, valid keys are {valid_fields}")
            )
    return errors


def _variable_check_type_is_valid(
        parent_path: str, obj: UnparsedJSON) -> ParseErrors:
    errors = []
    valid = ["string", "int", "bool", "index-list"]
    t = obj["type"]
    if t not in valid:
        errors.append(
            ParseError(
                _path(parent_path, "type"),
                f"'{t}' is an invalid value, valid values are {valid}"))
    return errors


def _variable_check_mode_is_valid(
        parent_path: str, obj: UnparsedJSON) -> ParseErrors:
    errors = []

    raw_mode = obj["mode"]
    try:
        Mode[raw_mode]
    except (KeyError) as e:
        valid = [m.value for m in Mode]
        errors.append(
            ParseError(
                _path(parent_path, "mode"),
                f"'{raw_mode}' is an invalid mode, valid modes are {valid}"))
    return errors


def _variable_check_values(parent_path: str, obj: UnparsedJSON) -> ParseErrors:
    errors = []

    if len(obj["values"]) == 0:
        errors.append(
            ParseError(_path(parent_path, "values"), "must not be empty"))

    i = -1
    for v in obj["values"]:
        i += 1
        if not isinstance(v, str):
            errors.append(
                ParseError(
                    _path(parent_path, "values", str(i)), "must be a string"))
    return errors


def _variable_check_values_not_defined(
        parent_path: str, obj: UnparsedJSON) -> ParseErrors:
    errors = []
    if "values" in obj:
        errors.append(
            ParseError(
                parent_path,
                "'values' can not be defined if 'regex' or 'regex_tests' is defined"
            ))
    return errors


def _variable_check_regex_fields_not_defined(
        parent_path: str, obj: UnparsedJSON) -> ParseErrors:
    errors = []
    if "regex" in obj or "regex_tests" in obj:
        errors.append(
            ParseError(
                parent_path,
                "'regex' and 'regex_tests' can not be defined if 'values' is defined"
            ))
    return errors


def _variable_check_regex_tests_defined(
        parent_path: str, obj: UnparsedJSON) -> ParseErrors:
    errors = []
    if "regex_tests" not in obj:
        errors.append(
            ParseError(
                parent_path,
                "'regex_tests' must be defined if 'regex' is defined"))
    return errors


def _variable_check_regex_defined(
        parent_path: str, obj: UnparsedJSON) -> ParseErrors:
    errors = []
    if "regex" not in obj:
        errors.append(
            ParseError(
                parent_path,
                "'regex' must be defined if 'regex_tests' is defined"))
    return errors


def _variable_check_regex_tests(
        parent_path: str, obj: UnparsedJSON) -> ParseErrors:
    errors = []
    i = -1
    for test in obj["regex_tests"]:
        i += 1
        index_path = _path(parent_path, "regex_tests", str(i))

        if not isinstance(test, Dict):
            errors.append(ParseError(index_path, "must be an object"))
        else:
            errors.extend(
                _ensure_no_extra_fields(
                    index_path, test, ["val", "should_match"]))
            _, errs = _parse_field(
                parent_path=index_path,
                key="val",
                json_type=str,
                required=True,
                obj=test)
            errors.extend(errs)
            _, errs = _parse_field(
                parent_path=index_path,
                key="should_match",
                json_type=bool,
                required=True,
                obj=test)
            errors.extend(errs)
    return errors
