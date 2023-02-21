from visitor import Group, Variable, RegexTest, ParseError, _path, _ensure_no_extra_fields, _parse_field, _parse_group, _parse_variable
import unittest


class TestPath(unittest.TestCase):

    def test_no_args(self):
        with self.assertRaises(ValueError) as e:
            _path()
        self.assertIn("must have at least one argument", str(e.exception))

    def test_one_args(self):
        got = _path("root")
        self.assertEqual(got, "root")

    def test_two_args(self):
        got = _path("root", "field")
        self.assertEqual(got, "root.field")

    def test_three_args(self):
        got = _path("root", "field", "0")
        self.assertEqual(got, "root.field.0")

    def test_empty_args(self):
        want_err = "arguments should not be the empty string"
        with self.assertRaises(ValueError) as e:
            _path("", "field")
        self.assertIn(want_err, str(e.exception))

        with self.assertRaises(ValueError):
            _path("root", "")
        self.assertIn(want_err, str(e.exception))

        with self.assertRaises(ValueError):
            _path("root", "field", "")
        self.assertIn(want_err, str(e.exception))


class TestEnsureNoExtraFieldPresent(unittest.TestCase):
    valid = ["one", "two"]

    def test_empty(self):
        got = _ensure_no_extra_fields("", {}, self.valid)
        self.assertEqual(got, [])

    def test_no_invalid(self):
        got = _ensure_no_extra_fields("", {"one": {}, "two": {}}, self.valid)
        self.assertEqual(got, [])

    def test_one_invalid(self):
        got = _ensure_no_extra_fields("", {"three": {}}, self.valid)
        self.assertEqual(
            got, [
                ParseError(
                    "",
                    "'three' is an invalid key, valid keys are ['one', 'two']")
            ])

    def test_two_invalid(self):
        got = _ensure_no_extra_fields(
            "root", {
                "three": {},
                "four": {}
            }, self.valid)
        self.assertEqual(
            got, [
                ParseError(
                    "root",
                    "'three' is an invalid key, valid keys are ['one', 'two']"),
                ParseError(
                    "root",
                    "'four' is an invalid key, valid keys are ['one', 'two']")
            ])

    def test_some_valid_some_invalid(self):
        got = _ensure_no_extra_fields(
            "root", {
                "one": {},
                "two": {},
                "three": {},
                "four": {}
            }, self.valid)
        self.assertEqual(
            got, [
                ParseError(
                    "root",
                    "'three' is an invalid key, valid keys are ['one', 'two']"),
                ParseError(
                    "root",
                    "'four' is an invalid key, valid keys are ['one', 'two']")
            ])


class TestParseField(unittest.TestCase):

    def test_json_type_not_valid(self):
        with self.assertRaises(ValueError) as e:
            _parse_field(
                parent_path="root",
                key="key",
                json_type=ParseError,
                required=False,
                obj={})
        self.assertIn("is not a valid json_type", str(e.exception))

    def test_custom_type_provided_but_not_extract_fn(self):
        with self.assertRaises(ValueError) as e:
            _parse_field(
                parent_path="root",
                key="key",
                json_type=list,
                required=False,
                obj={"key": []},
                return_type=list[str])
        self.assertIn(
            "'extract_fn' must be provided if 'return_type' is provided",
            str(e.exception))

    def test_extract_fn_provided_but_custom_type_not_provided(self):
        with self.assertRaises(ValueError) as e:
            _parse_field(
                parent_path="root",
                key="key",
                json_type=list,
                required=False,
                obj={"key": []},
                extract_fn=lambda o: [])
        self.assertIn(
            "'return_type' must be provided if 'extract_fn' is provided",
            str(e.exception))

    def test_default_provided_but_field_is_required(self):
        with self.assertRaises(ValueError) as e:
            _parse_field(
                parent_path="root",
                key="key",
                json_type=list,
                required=True,
                obj={"keyz": []},
                default=["Default"])
        self.assertIn(
            "'default' must be None if 'required' is True", str(e.exception))

    def test_required_field_not_present(self):
        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=True,
            obj={"keyz": "are typos"})
        self.assertEqual(
            gotErrors, [ParseError("root", "'key' is a required field")])
        self.assertEqual(got, None)

    def test_required_field_is_present(self):
        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=True,
            obj={"key": "fixed typo :)"})
        self.assertEqual(gotErrors, [])
        self.assertEqual(got, "fixed typo :)")

    def test_unrequired_field_not_present(self):
        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=False,
            obj={"other_key": "chillin"})
        self.assertEqual(gotErrors, [])
        self.assertEqual(got, None)

    def test_unrequired_field_is_present(self):
        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=False,
            obj={"key": "on vacay"})
        self.assertEqual(gotErrors, [])
        self.assertEqual(got, "on vacay")

    def test_parses_str_wrong_type(self):
        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=False,
            obj={"key": True})
        self.assertEqual(
            gotErrors, [ParseError("root.key", "must be a string")])
        self.assertEqual(got, None)

    def test_parses_str(self):
        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=False,
            obj={"key": "was parsed"})
        self.assertEqual(gotErrors, [])
        self.assertEqual(got, "was parsed")

    def test_parses_bool_wrong_type(self):
        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=bool,
            required=False,
            obj={"key": "true"})
        self.assertEqual(gotErrors, [ParseError("root.key", "must be a bool")])
        self.assertEqual(got, None)

    def test_parses_false_bool(self):
        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=bool,
            required=False,
            obj={"key": False})
        self.assertEqual(gotErrors, [])
        self.assertEqual(got, False)

    def test_parses_true_bool(self):
        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=bool,
            required=False,
            obj={"key": True})
        self.assertEqual(gotErrors, [])
        self.assertEqual(got, True)

    def test_parses_dict_wrong_type(self):
        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=dict,
            required=False,
            obj={"key": "true"})
        self.assertEqual(
            gotErrors, [ParseError("root.key", "must be a object")])
        self.assertEqual(got, None)

    def test_parses_dict(self):
        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=dict,
            required=False,
            obj={"key": {
                "subkey": "hello there"
            }})
        self.assertEqual(gotErrors, [])
        self.assertEqual(got, {"subkey": "hello there"})

    def test_parses_list_wrong_type(self):
        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=list,
            required=False,
            obj={"key": "true"})
        self.assertEqual(gotErrors, [ParseError("root.key", "must be a list")])
        self.assertEqual(got, None)

    def test_parses_list(self):
        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=list,
            required=False,
            obj={"key": [1, 2, 3]})
        self.assertEqual(gotErrors, [])
        self.assertEqual(got, [1, 2, 3])

    def test_check_not_called_if_unrequired_field_not_present(self):
        called = False

        def check(parent_path, obj):
            called = True
            return []

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=False,
            obj={"keyz": "abc"},
            checks=[check])
        self.assertEqual(gotErrors, [])
        self.assertEqual(got, None)
        self.assertEqual(called, False)

    def test_check_not_called_if_required_field_not_present(self):
        called = False

        def check(parent_path, obj):
            called = True
            return []

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=True,
            obj={"keyz": "abc"},
            checks=[check])
        self.assertEqual(
            gotErrors, [ParseError("root", "'key' is a required field")])
        self.assertEqual(got, None)
        self.assertEqual(called, False)

    def test_check_not_called_if_unrequired_field_wrong_type(self):
        called = False

        def check(parent_path, obj):
            nonlocal called
            called = True
            return []

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=False,
            obj={"key": 3},
            checks=[check])
        self.assertEqual(
            gotErrors, [ParseError("root.key", "must be a string")])
        self.assertEqual(got, None)
        self.assertEqual(called, False)

    def test_check_not_called_if_required_field_wrong_type(self):
        called = False

        def check(parent_path, obj):
            nonlocal called
            called = True
            return []

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=True,
            obj={"key": 4},
            checks=[check])
        self.assertEqual(
            gotErrors, [ParseError("root.key", "must be a string")])
        self.assertEqual(got, None)
        self.assertEqual(called, False)

    def test_check_called_if_unrequired_field_present(self):
        called = False

        def check(parent_path, obj):
            nonlocal called
            called = True
            return []

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=False,
            obj={"key": "abc"},
            checks=[check])
        self.assertEqual(gotErrors, [])
        self.assertEqual(got, "abc")
        self.assertEqual(called, True)

    def test_check_called_if_required_field_present(self):
        called = False

        def check(parent_path, obj):
            nonlocal called
            called = True
            return []

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=True,
            obj={"key": "abc"},
            checks=[check])
        self.assertEqual(gotErrors, [])
        self.assertEqual(got, "abc")
        self.assertEqual(called, True)

    def test_all_checks_called(self):
        a_called = False

        def check_a(parent_path, obj):
            nonlocal a_called
            a_called = True
            return []

        b_called = False

        def check_b(parent_path, obj):
            nonlocal b_called
            b_called = True
            return []

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=True,
            obj={"key": "abc"},
            checks=[check_a, check_b])
        self.assertEqual(gotErrors, [])
        self.assertEqual(got, "abc")
        self.assertEqual(a_called, True)
        self.assertEqual(b_called, True)

    def test_check_error_is_returned(self):

        def check(parent_path, obj):
            return [ParseError(parent_path, "error")]

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=True,
            obj={"key": "abc"},
            checks=[check])
        self.assertEqual(gotErrors, [ParseError("root", "error")])
        self.assertEqual(got, None)

    def test_check_errors_are_returned(self):

        def check(parent_path, obj):
            return [
                ParseError(parent_path, "error1"),
                ParseError(parent_path, "error2")
            ]

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=True,
            obj={"key": "abc"},
            checks=[check])
        self.assertEqual(
            gotErrors,
            [ParseError("root", "error1"),
             ParseError("root", "error2")])
        self.assertEqual(got, None)

    def test_errors_from_multiple_checks_are_returned(self):

        def check_a(parent_path, obj):
            return [ParseError(parent_path, "a")]

        def check_b(parent_path, obj):
            return [
                ParseError(parent_path, "b1"),
                ParseError(parent_path, "b2")
            ]

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=True,
            obj={"key": "abc"},
            checks=[check_a, check_b])
        self.assertEqual(
            gotErrors, [
                ParseError("root", "a"),
                ParseError("root", "b1"),
                ParseError("root", "b2")
            ])
        self.assertEqual(got, None)

    def test_extract_fn_not_called_if_unrequired_field_not_present(self):
        called = False

        def extract(obj):
            nonlocal called
            called = True
            return "extract"

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=False,
            obj={"keyz": "abc"},
            return_type=str,
            extract_fn=extract)
        self.assertEqual(got, None)
        self.assertEqual(called, False)
        self.assertEqual(gotErrors, [])

    def test_extract_fn_not_called_if_required_field_not_present(self):
        called = False

        def extract(obj):
            nonlocal called
            called = True
            return "extract"

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=True,
            obj={"keyz": "abc"},
            return_type=str,
            extract_fn=extract)
        self.assertEqual(
            gotErrors, [ParseError("root", "'key' is a required field")])
        self.assertEqual(got, None)
        self.assertEqual(called, False)

    def test_extract_fn_not_called_if_unrequired_field_wrong_type(self):
        called = False

        def extract(obj):
            nonlocal called
            called = True
            return "extract"

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=False,
            obj={"key": 3},
            return_type=str,
            extract_fn=extract)
        self.assertEqual(
            gotErrors, [ParseError("root.key", "must be a string")])
        self.assertEqual(got, None)
        self.assertEqual(called, False)

    def test_extract_fn_not_called_if_required_field_wrong_type(self):
        called = False

        def extract(obj):
            nonlocal called
            called = True
            return "extract"

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=True,
            obj={"key": 4},
            return_type=str,
            extract_fn=extract)
        self.assertEqual(
            gotErrors, [ParseError("root.key", "must be a string")])
        self.assertEqual(got, None)
        self.assertEqual(called, False)

    def test_extract_fn_not_called_if_check_returns_errors(self):

        def check(parent_path, obj):
            return [ParseError(parent_path, "error")]

        called = False

        def extract(obj):
            nonlocal called
            called = True
            return "extract"

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=True,
            obj={"key": "abc"},
            checks=[check],
            return_type=str,
            extract_fn=extract)
        self.assertEqual(gotErrors, [ParseError("root", "error")])
        self.assertEqual(got, None)
        self.assertEqual(called, False)

    def test_extract_fn_not_called_if_some_checks_return_errors(self):

        def check_a(parent_path, obj):
            return []

        def check_b(parent_path, obj):
            return [ParseError(parent_path, "error")]

        called = False

        def extract(obj):
            nonlocal called
            called = True
            return "extract"

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=True,
            obj={"key": "abc"},
            checks=[check_a, check_b],
            return_type=str,
            extract_fn=extract)
        self.assertEqual(gotErrors, [ParseError("root", "error")])
        self.assertEqual(got, None)
        self.assertEqual(called, False)

    def test_extract_fn_called_if_unrequired_field_present(self):

        def extract(obj):
            return "extract"

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=False,
            obj={"key": "abc"},
            return_type=str,
            extract_fn=extract)
        self.assertEqual(gotErrors, [])
        self.assertEqual(got, "extract")

    def test_extract_fn_called_if_required_field_present(self):

        def extract(obj):
            return "extract"

        got, gotErrors = _parse_field(
            parent_path="root",
            key="key",
            json_type=str,
            required=True,
            obj={"key": "abc"},
            return_type=str,
            extract_fn=extract)
        self.assertEqual(gotErrors, [])
        self.assertEqual(got, "extract")


class TestParseGroup(unittest.TestCase):

    def test_empty(self):
        got, gotErrors = _parse_group("root", {})
        self.assertEqual(
            gotErrors, [
                ParseError("root", "'group_description' is a required field"),
                ParseError("root", "'members' is a required field")
            ])
        self.assertEqual(got, None)

    def test_no_description(self):
        got, gotErrors = _parse_group("root", {"members": {}})
        self.assertEqual(
            gotErrors, [
                ParseError("root", "'group_description' is a required field"),
            ])
        self.assertEqual(got, None)

    def test_description_wrong_type(self):
        got, gotErrors = _parse_group(
            "root", {
                "group_description": True,
                "members": {}
            })
        self.assertEqual(
            gotErrors, [
                ParseError("root.group_description", "must be a string"),
            ])
        self.assertEqual(got, None)

    def test_no_members(self):
        got, gotErrors = _parse_group("root", {"group_description": "A group"})
        self.assertEqual(
            gotErrors, [
                ParseError("root", "'members' is a required field"),
            ])
        self.assertEqual(got, None)

    def test_members_wrong_type(self):
        got, gotErrors = _parse_group(
            "root", {
                "group_description": "A group",
                "members": []
            })
        self.assertEqual(
            gotErrors, [
                ParseError("root.members", "must be a object"),
            ])
        self.assertEqual(got, None)

    def test_has_required_but_also_extra_fields(self):
        got, gotErrors = _parse_group(
            "root", {
                "group_description": "A group",
                "members": {
                    "MY_VAR": {}
                },
                "extra": "hi!"
            })
        self.assertEqual(
            gotErrors, [
                ParseError(
                    "root",
                    "'extra' is an invalid key, valid keys are ['group_description', 'members']"
                )
            ])
        self.assertEqual(got, None)

    def test_has_only_extra_fields(self):
        got, gotErrors = _parse_group(
            "root", {
                "extra": "hi!",
                "MY_VAR": "is cool"
            })
        self.assertEqual(
            gotErrors, [
                ParseError(
                    "root",
                    "'extra' is an invalid key, valid keys are ['group_description', 'members']"
                ),
                ParseError(
                    "root",
                    "'MY_VAR' is an invalid key, valid keys are ['group_description', 'members']"
                ),
                ParseError("root", "'group_description' is a required field"),
                ParseError("root", "'members' is a required field")
            ])
        self.assertEqual(got, None)

    def test_valid(self):
        got, gotErrors = _parse_group(
            "root", {
                "group_description": "A group",
                "members": {
                    "MY_VAR": {}
                }
            })
        self.assertEqual(gotErrors, [])
        self.assertEqual(got, Group("A group", {"MY_VAR": {}}))


class TestParseVariable(unittest.TestCase):
    basevar = {"description": "Var", "type": "string"}
    basetests = [
        {
            "val": "hiya",
            "should_match": True
        }, {
            "val": "hmmz",
            "should_match": False
        }
    ]
    basetests_parsed = [RegexTest("hiya", True), RegexTest("hmmz", False)]

    def test_empty(self):
        got, gotErrors = _parse_variable("root", {})
        self.assertEqual(
            gotErrors, [
                ParseError("root", "'description' is a required field"),
                ParseError("root", "'type' is a required field")
            ])
        self.assertEqual(got, None)

    def test_no_description(self):
        got, gotErrors = _parse_variable("root", {"type": "string"})
        self.assertEqual(
            gotErrors, [
                ParseError("root", "'description' is a required field"),
            ])
        self.assertEqual(got, None)

    def test_description_wrong_type(self):
        got, gotErrors = _parse_variable(
            "root", {
                "description": {},
                "type": "string"
            })
        self.assertEqual(
            gotErrors, [
                ParseError("root.description", "must be a string"),
            ])
        self.assertEqual(got, None)

    def test_no_type(self):
        got, gotErrors = _parse_variable("root", {"description": "A var"})
        self.assertEqual(
            gotErrors, [ParseError("root", "'type' is a required field")])
        self.assertEqual(got, None),

    def test_type_wrong_type(self):
        got, gotErrors = _parse_variable(
            "root", {
                "description": "A var",
                "type": True
            })
        self.assertEqual(
            gotErrors, [ParseError("root.type", "must be a string")])
        self.assertEqual(got, None)

    def test_type_invalid_value(self):
        got, gotErrors = _parse_variable(
            "root", {
                "description": "A var",
                "type": "cooleo"
            })
        self.assertEqual(got, None),
        self.assertEqual(
            gotErrors, [
                ParseError(
                    "root.type",
                    "'cooleo' is an invalid value, valid values are ['string', 'int', 'bool', 'index-list']"
                )
            ])

    def test_type_valid_values(self):
        for v in ["string", "int", "bool", "index-list"]:
            got, gotErrors = _parse_variable(
                "root", {
                    "description": "A var",
                    "type": v
                })
            self.assertEqual(gotErrors, [])
            self.assertEqual(got, Variable("A var", v, False, None, None, None))

    def test_unrequired_fields_not_set(self):
        got, gotErrors = _parse_variable("root", self.basevar)
        self.assertEqual(gotErrors, [])
        self.assertEqual(
            got, Variable("Var", "string", False, None, None, None))

    def test_required_wrong_type(self):
        v = dict(self.basevar, **{"required": "true"})
        got, gotErrors = _parse_variable("root", v)
        self.assertEqual(
            gotErrors, [ParseError("root.required", "must be a bool")])
        self.assertEqual(got, None)

    def test_required_false(self):
        v = dict(self.basevar, **{"required": False})
        got, gotErrors = _parse_variable("root", v)
        self.assertEqual(gotErrors, [])
        self.assertEqual(
            got, Variable("Var", "string", False, None, None, None))

    def test_required_true(self):
        v = dict(self.basevar, **{"required": True})
        got, gotErrors = _parse_variable("root", v)
        self.assertEqual(gotErrors, [])
        self.assertEqual(got, Variable("Var", "string", True, None, None, None))

    def test_values_wrong_type(self):
        v = dict(self.basevar, **{"values": "only one"})
        got, gotErrors = _parse_variable("root", v)
        self.assertEqual(
            gotErrors, [ParseError("root.values", "must be a list")])
        self.assertEqual(got, None)

    def test_values_empty(self):
        v = dict(self.basevar, **{"values": []})
        got, gotErrors = _parse_variable("root", v)
        self.assertEqual(
            gotErrors, [ParseError("root.values", "must not be empty")])
        self.assertEqual(got, None)

    def test_values_not_all_strs(self):
        v = dict(self.basevar, **{"values": ["one", 2]})
        got, gotErrors = _parse_variable("root", v)
        self.assertEqual(
            gotErrors, [ParseError("root.values.1", "must be a string")])
        self.assertEqual(got, None)

    def test_values_valid(self):
        v = dict(self.basevar, **{"values": ["one", "two"]})
        got, gotErrors = _parse_variable("root", v)
        self.assertEqual(gotErrors, [])
        self.assertEqual(
            got, Variable("Var", "string", False, ["one", "two"], None, None))

    def test_regex_wrong_type(self):
        v = dict(self.basevar, **{"regex": []})
        got, gotErrors = _parse_variable("root", v)
        self.assertEqual(
            gotErrors, [ParseError("root.regex", "must be a string")])
        self.assertEqual(got, None)

    def test_regex_tests_wrong_type(self):
        v = dict(self.basevar, **{"regex_tests": {}})
        got, gotErrors = _parse_variable("root", v)
        self.assertEqual(
            gotErrors, [ParseError("root.regex_tests", "must be a list")])
        self.assertEqual(got, None)

    def test_regex_no_tests(self):
        v = dict(self.basevar, **{"regex": ".*"})
        got, gotErrors = _parse_variable("root", v)
        self.assertEqual(
            gotErrors, [
                ParseError(
                    "root",
                    "'regex_tests' must be defined if 'regex' is defined")
            ])
        self.assertEqual(got, None)

    def test_regex_tests_no_regex(self):
        v = dict(self.basevar, **{"regex_tests": self.basetests})
        got, gotErrors = _parse_variable("root", v)
        self.assertEqual(
            gotErrors, [
                ParseError(
                    "root",
                    "'regex' must be defined if 'regex_tests' is defined")
            ])
        self.assertEqual(got, None)

    def test_regex_and_values(self):
        v = dict(
            self.basevar, **{
                "values": ["one"],
                "regex": ".*",
                "regex_tests": []
            })
        got, gotErrors = _parse_variable("root", v)
        self.assertEqual(
            gotErrors, [
                ParseError(
                    "root",
                    "'regex' can not be defined if 'values' is defined"),
                ParseError(
                    "root",
                    "'regex_tests' can not be defined if 'values' is defined"),
                ParseError(
                    "root",
                    "'values' can not be defined if 'regex' or 'regex_tests' is defined"
                )
            ])
        self.assertEqual(got, None)

    def test_regex_and_tests_valid(self):
        v = dict(self.basevar, **{"regex": ".*", "regex_tests": self.basetests})
        got, gotErrors = _parse_variable("root", v)
        self.assertEqual(gotErrors, [])
        self.assertEqual(
            got,
            Variable("Var", "string", False, None, ".*", self.basetests_parsed))


if __name__ == "__main__":
    unittest.main()
