import unittest

from config_loader import ConfigLoader
"""
 Tests for the ConfigLoader, calls the I/O methods to match the actual
 experience of running the class.

 To run the tests: PYTHONPATH="${PYTHONPATH}:${pwd}" python3 cloud/shared/bin/lib/config_loader_test.py
"""


class TestConfigLoader(unittest.TestCase):

    def test_validate_config_for_not_including_required(self):
        defs = {
            "FOO": {
                "required": True,
                "secret": False,
                "type": "string"
            },
            "Bar": {
                "required": True,
                "secret": False,
                "type": "string"
            },
            "Bat": {
                "required": False,
                "secret": False,
                "type": "string"
            },
        }

        configs = {"FOO": "test"}

        config_loader = ConfigLoader()
        config_loader.variable_definitions = defs
        config_loader.configs = configs

        self.assertEqual(
            config_loader.validate_config(),
            ["[Bar] required, but not provided"])

    def test_validate_config_for_incorrect_enums(self):
        defs = {
            "FOO":
                {
                    "required": True,
                    "secret": False,
                    "type": "enum",
                    "values": ["abc", "def"],
                },
        }
        configs = {"FOO": "test"}

        config_loader = ConfigLoader()
        config_loader.variable_definitions = defs
        config_loader.configs = configs

        self.assertEqual(
            config_loader.validate_config(), [
                "[FOO] 'test' is not a supported enum value. Want a value in [abc, def]"
            ])

    def test_validate_config_for_correct_enums(self):
        defs = {
            "FOO":
                {
                    "required": True,
                    "secret": False,
                    "type": "enum",
                    "values": ["abc"],
                },
        }
        configs = {"FOO": "abc"}
        config_loader = ConfigLoader()
        config_loader.variable_definitions = defs
        config_loader.configs = configs

        self.assertEqual(config_loader.validate_config(), [])

    def test_validate_config_for_empty_enum(self):
        defs = {
            "FOO":
                {
                    "required": True,
                    "secret": False,
                    "type": "enum",
                    "values": ["abc"],
                },
        }

        configs = {"FOO": ""}
        config_loader = ConfigLoader()
        config_loader.variable_definitions = defs
        config_loader.configs = configs

        self.assertEqual(
            config_loader.validate_config(),
            ["[FOO] '' is not a supported enum value. Want a value in [abc]"])

    def test_value_regex(self):
        config_loader = ConfigLoader()
        config_loader.variable_definitions = {
            "FOO":
                {
                    "required": True,
                    "secret": False,
                    "type": "string",
                    "value_regex": "[a-z]+",
                    "value_regex_error_message": "some message"
                },
        }

        config_loader.configs = {"FOO": "somenumbers123"}
        self.assertEqual(
            config_loader.validate_config(),
            ['[FOO] \'somenumbers123\' not valid: some message'])

    def test_value_regex_ignored_for_not_required_and_not_provided(self):
        config_loader = ConfigLoader()
        config_loader.variable_definitions = {
            "FOO":
                {
                    "required": False,
                    "secret": False,
                    "type": "string",
                    "value_regex": "[a-z]+",
                    "value_regex_error_message": "some message"
                },
        }

        config_loader.configs = {}
        self.assertEqual(config_loader.validate_config(), [])


if __name__ == "__main__":
    unittest.main()
