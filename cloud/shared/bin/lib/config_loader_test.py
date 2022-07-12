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
            ["Bar is required, but not provided"])

    def test_validate_config_for_incorrect_enums(self):
        defs = {
            "FOO":
                {
                    "required": True,
                    "secret": False,
                    "type": "enum",
                    "values": ["abc"],
                },
        }
        configs = {"FOO": "test"}

        config_loader = ConfigLoader()
        config_loader.variable_definitions = defs
        config_loader.configs = configs

        self.assertEqual(
            config_loader.validate_config(),
            ["test not supported enum for FOO"])

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
            config_loader.validate_config(), [" not supported enum for FOO"])


if __name__ == "__main__":
    unittest.main()
