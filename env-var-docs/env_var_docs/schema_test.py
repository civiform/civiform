import importlib.resources
import json
import jsonschema
import unittest


class TestSchema(unittest.TestCase):

    def test_valid_schema(self):
        # Test that these calls do not throw exceptions.
        schema = json.loads(
            importlib.resources.files("schema").joinpath(
                "schema.json").read_text())
        jsonschema.Draft7Validator.check_schema(schema)


if __name__ == "__main__":
    unittest.main()
