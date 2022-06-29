import unittest

from validate_variable_definitions import ValidateVariableDefinitions


class TestValidateVariableDefinitions(unittest.TestCase):

    def test_validate_all_variable_definitions(self):
        validator = ValidateVariableDefinitions()
        validator.load_repo_variable_definitions_files()
        errors = validator.get_validation_errors()

        self.assertEqual(errors, {})

    def test_get_validation_errors_float_no_errors(self):
        defs = {
            "FOO":
                {
                    "required": True,
                    "secret": False,
                    "tfvar": False,
                    "type": "float"
                }
        }

        errors = ValidateVariableDefinitions(defs).get_validation_errors()

        self.assertEqual(errors, {})

    def test_get_validation_errors_integer_no_errors(self):
        defs = {
            "FOO":
                {
                    "required": True,
                    "secret": False,
                    "tfvar": False,
                    "type": "integer"
                }
        }

        errors = ValidateVariableDefinitions(defs).get_validation_errors()

        self.assertEqual(errors, {})

    def test_get_validation_errors_string_no_errors(self):
        defs = {
            "FOO":
                {
                    "required": True,
                    "secret": False,
                    "tfvar": False,
                    "type": "string"
                }
        }

        errors = ValidateVariableDefinitions(defs).get_validation_errors()

        self.assertEqual(errors, {})

    def test_get_validation_errors_string_missing_secret(self):
        defs = {"FOO": {"required": True, "tfvar": False, "type": "string"}}

        errors = ValidateVariableDefinitions(defs).get_validation_errors()

        self.assertEqual(errors, {"FOO": ["Missing 'secret' field."]})

    def test_get_validation_errors_string_missing_type(self):
        defs = {"FOO": {"required": True, "tfvar": False, "secret": False}}

        errors = ValidateVariableDefinitions(defs).get_validation_errors()

        self.assertEqual(errors, {"FOO": ["Missing 'type' field."]})

    def test_get_validation_errors_string_missing_required(self):
        defs = {"FOO": {"secret": False, "tfvar": False, "type": "string"}}

        errors = ValidateVariableDefinitions(defs).get_validation_errors()

        self.assertEqual(errors, {"FOO": ["Missing 'required' field."]})

    def test_get_validation_errors_enum_no_errors(self):
        defs = {
            "CIVIFORM_CLOUD_PROVIDER":
                {
                    "required": True,
                    "secret": False,
                    "tfvar": False,
                    "type": "enum",
                    "values": ["gcp", "azure", "aws"]
                }
        }

        errors = ValidateVariableDefinitions(defs).get_validation_errors()

        self.assertEqual(errors, {})

    def test_get_validation_errors_enum_missing_values(self):
        defs = {
            "CIVIFORM_CLOUD_PROVIDER":
                {
                    "required": True,
                    "secret": False,
                    "tfvar": False,
                    "type": "enum"
                }
        }

        errors = ValidateVariableDefinitions(defs).get_validation_errors()

        expected_errors = {
            "CIVIFORM_CLOUD_PROVIDER": ["Missing 'values' field for enum."]
        }

        self.assertEqual(errors, expected_errors)

    def test_get_validation_errors_tf_var_missing(self):
        defs = {
            "CIVIFORM_CLOUD_PROVIDER":
                {
                    "required": True,
                    "secret": False,
                    "type": "string"
                }
        }

        errors = ValidateVariableDefinitions(defs).get_validation_errors()

        expected_errors = {
            "CIVIFORM_CLOUD_PROVIDER": ["Missing 'tfvar' field."]
        }

        self.assertEqual(errors, expected_errors)


if __name__ == '__main__':
    unittest.main()
