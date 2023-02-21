"""Provides functionality for parsing and visiting an environment variable
documentation file.
"""

import dataclasses
import typing
import json

UnparsedJSON = dict[str, typing.Any]


@dataclasses.dataclass
class Group:
    group_description: str
    members: dict[str, UnparsedJSON]


@dataclasses.dataclass
class RegexTest:
    val: str
    should_match: bool


@dataclasses.dataclass
class Variable:
    description: str
    type: str
    required: bool
    values: list[str] | None
    regex: str | None
    regex_tests: list[RegexTest] | None


@dataclasses.dataclass
class NodeInfo:
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

    details: Group | Variable
    """The group details or variable details."""


VisitFn = typing.Callable[[NodeInfo], None]
"""A function that takes a NodeInfo and does something with it."""


@dataclasses.dataclass
class ParseError:
    """
    As the environment variable documentation file is parsed, any invalid
    structures are reported through ParseErrors.
    """

    path: str
    """The JSON path of the invalid object."""
    msg: str
    """"Why the object is invalid."""


ParseErrors = list[ParseError]


def visit(file: typing.TextIO, visit_fn: VisitFn) -> ParseErrors:
    """Parses an environment variable documentation file. See README.md for the
    expected structure.

    The file is completely parsed before any visit_fn is run. If there are
    parsing errors, no visit_fn is run and the errors are returned.

    If the file has no parsing errors, visit_fn is called on each node in
    preorder traversal (visit node then its children) order.
    """
    file.seek(0)  # Ensure we are reading from the beginning of the file.

    json_root = "file"
    try:
        docs = json.load(file)
    except json.JSONDecodeError as e:
        return [ParseError(json_root, f"not valid JSON: {e.msg}")]
    if not isinstance(docs, dict):
        return [ParseError(json_root, "is not a JSON object")]

    nodes_to_visit: list[NodeInfo] = []
    parsing_errors: ParseErrors = []

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
        nodes_to_visit: list[NodeInfo], parsing_errors: ParseErrors):
    """Recursively visits a node and its children, in preorder traversal order (visit root then children)."""

    # Is this a group?
    group, group_errs = _parse_group(parent_path, value)
    if len(group_errs) == 0:
        assert group is not None  # appease mypy
        nodes_to_visit.append(
            NodeInfo(
                level=level, json_path=parent_path, name=key, details=group))

        # Recurse into group members
        for key, value in group.members.items():
            _recursively_parse(
                level + 1, f"{parent_path}.{key}", key, value, nodes_to_visit,
                parsing_errors)
        return

    # Is this a variable?
    var, var_errs = _parse_variable(parent_path, value)
    if len(var_errs) == 0:
        assert var is not None  # appease mypy
        nodes_to_visit.append(
            NodeInfo(level=level, json_path=parent_path, name=key, details=var))
        return

    # details is neither a group nor a variable.
    parsing_errors.append(
        ParseError(
            f"{parent_path}.{key}",
            "Could not parse a Group or Variable. Group parse errors: {group_errs}. Variable parse errors: {var_errs}"
        ))


def _parse_group(parent_path: str,
                 obj: UnparsedJSON) -> tuple[Group | None, ParseErrors]:
    """Parses a Group from obj. If ParseErrors are encountered, None is returned."""

    errors = []
    errors.extend(
        _ensure_no_extra_fields(
            parent_path, obj, ["group_description", "members"]))

    group_description, errs = _parse_field(
        parent_path=parent_path,
        key="group_description",
        json_type=str,
        required=True,
        obj=obj)
    errors.extend(errs)

    members, errs = _parse_field(
        parent_path=parent_path,
        key="members",
        json_type=dict,
        required=True,
        obj=obj)
    errors.extend(errs)

    if len(errors) != 0:
        return None, errors
    else:
        # appease mypy
        assert group_description is not None
        assert members is not None
        return Group(group_description, members), []


def _parse_variable(parent_path: str,
                    obj: UnparsedJSON) -> tuple[Variable | None, ParseErrors]:
    """Parses a Variable from obj. If ParseErrors are encountered, None is returned."""

    errors = []
    errors.extend(
        _ensure_no_extra_fields(
            parent_path, obj, [
                "description", "type", "required", "values", "regex",
                "regex_tests"
            ]))

    description, errs = _parse_field(
        parent_path=parent_path,
        key="description",
        json_type=str,
        required=True,
        obj=obj)
    errors.extend(errs)

    type, errs = _parse_field(
        parent_path=parent_path,
        key="type",
        json_type=str,
        required=True,
        obj=obj,
        checks=[_variable_check_type_is_valid])
    errors.extend(errs)

    required, errs = _parse_field(
        parent_path=parent_path,
        key="required",
        json_type=bool,
        required=False,
        obj=obj,
        default=False)
    errors.extend(errs)

    def convert_values(obj: UnparsedJSON) -> list[str]:
        out = []
        for v in obj["values"]:
            out.append(v)
        return out

    values, errs = _parse_field(
        parent_path=parent_path,
        key="values",
        json_type=list,
        required=False,
        obj=obj,
        checks=[_variable_check_values, _variable_check_regex_fields_not_defined],
        return_type=list[str],
        extract_fn=convert_values)
    errors.extend(errs)

    regex, errs = _parse_field(
        parent_path=parent_path,
        key="regex",
        json_type=str,
        required=False,
        obj=obj,
        checks=[_variable_check_values_not_defined, _variable_check_regex_tests_defined])
    errors.extend(errs)

    def convert_regex_tests(obj: UnparsedJSON) -> list[RegexTest]:
        out = []
        for test in obj["regex_tests"]:
            out.append(RegexTest(test["val"], test["should_match"]))
        return out

    regex_tests, errs = _parse_field(
        parent_path=parent_path,
        key="regex_tests",
        json_type=list,
        required=False,
        obj=obj,
        checks=[_variable_check_regex_defined, _variable_check_regex_tests],
        return_type=list[RegexTest],
        extract_fn=convert_regex_tests)
    errors.extend(errs)

    if len(errors) != 0:
        return None, errors
    else:
        # appease mypy
        assert description is not None
        assert type is not None
        assert required is not None
        return Variable(
            description, type, required, values, regex, regex_tests), []


T = typing.TypeVar("T")
CheckFn = typing.Callable[[str, UnparsedJSON], ParseErrors]
"""A CheckFn takes in a parent_path and an object and returns any ParseErrors."""


ExtractFn = typing.Callable[[UnparsedJSON], T]
"""An ExtractFn takes in an object and returns a complex type."""


def _parse_field(
        parent_path: str,
        key: str,
        json_type: type,
        required: bool,
        obj: UnparsedJSON,
        default: T | None = None,
        checks: list[CheckFn] = [],
        return_type: typing.Type[T] | None = None,
        extract_fn: ExtractFn | None = None) -> tuple[T | None, ParseErrors]:
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
      json_type: The type expected at key. Must be one of [str, bool, dict, list] or ValueError is raised.
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
    if json_type not in [str, bool, dict, list]:
        raise ValueError(
            f"'{json_type}' is not a valid json_type, valid types are [str, bool, dict, list]"
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
            if json_type == dict:
                t = "object"
            if json_type == list:
                t = "list"
            errors.append(ParseError(_path(parent_path, key), f"must be a {t}"))
        else:
            for check in checks:
                errors.extend(check(parent_path, obj))
            if len(errors) == 0:
                value = obj[key]
                if extract_fn is not None:
                    value = extract_fn(obj)

    return value, errors


def _path(*args: str) -> str:
    """Creates a path from segments. Segments can not be the empty string."""
    if len(args) == 0:
        raise ValueError("must have at least one argument")
    for a in args:
        if a == "":
            raise ValueError("arguments should not be the empty string")

    return ".".join(args)


def _ensure_no_extra_fields(
        parent_path: str, obj: UnparsedJSON,
        valid_fields: list[str]) -> ParseErrors:
    """Returns ParseErrors for any keys in obj that are not in valid_fields."""
    errors = []
    for key in list(obj):
        if key not in valid_fields:
            errors.append(
                ParseError(
                    parent_path,
                    f"'{key}' is an invalid key, valid keys are {valid_fields}"))
    return errors


def _variable_check_type_is_valid(parent_path: str, obj: UnparsedJSON) -> ParseErrors:
    errors = []
    valid = ["string", "int", "bool", "index-list"]
    t = obj["type"]
    if t not in valid:
        errors.append(
            ParseError(
                _path(parent_path, "type"),
                f"'{t}' is an invalid value, valid values are {valid}"))
    return errors

def _variable_check_values(parent_path: str, obj: UnparsedJSON) -> ParseErrors:
    errors = []

    if len(obj["values"]) == 0:
        errors.append(ParseError(_path(parent_path, "values"), "must not be empty"))

    i = -1
    for v in obj["values"]:
        i += 1
        if not isinstance(v, str):
            errors.append(
                ParseError(
                    _path(parent_path, "values", str(i)),
                    "must be a string"))
    return errors

def _variable_check_values_not_defined(parent_path: str, obj: UnparsedJSON) -> ParseErrors:
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
    if "regex" in obj:
        errors.append(
            ParseError(
                parent_path, "'regex' can not be defined if 'values' is defined"))
    if "regex_tests" in obj:
        errors.append(
            ParseError(
                parent_path,
                "'regex_tests' can not be defined if 'values' is defined"))
    return errors

def _variable_check_regex_tests_defined(parent_path: str, obj: UnparsedJSON) -> ParseErrors:
    errors = []
    if "regex_tests" not in obj:
        errors.append(
                ParseError(
                    parent_path,
                    "'regex_tests' must be defined if 'regex' is defined"))
    return errors

def _variable_check_regex_defined(parent_path: str, obj: UnparsedJSON) -> ParseErrors:
    errors = []
    if "regex" not in obj:
        errors.append(
                ParseError(
                    parent_path,
                    "'regex' must be defined if 'regex_tests' is defined"))
    return errors

def _variable_check_regex_tests(parent_path: str, obj: UnparsedJSON) -> ParseErrors:
    errors = []
    i = -1
    for test in obj["regex_tests"]:
        i += 1
        index_path = _path(parent_path, "regex_tests", str(i))

        if not isinstance(test, dict):
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
