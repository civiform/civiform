import os
import glob
import json

class ValidateVariableDefinitions():
    def __init__(self, variable_definitions = {}):
        self.variable_definitions = variable_definitions

    def load_repo_variable_definitions_files(self):
        self.variable_definitions = {}
        cwd = os.getcwd()

        definition_file_paths = glob.glob(cwd + '/cloud/**/variable_definitions.json', recursive=True)

        for path in definition_file_paths:
            with open(path, 'r') as file:
                definitions = json.loads(file.read())

                for name, definition in definitions.items():
                    if name in self.variable_definitions:
                        raise RuntimeError(f"Duplicate variable name: {name} at {path}")

                    self.variable_definitions[name] = definition

    def get_validation_errors(self):
        all_errors = {}

        for name, definition in self.variable_definitions.items():
            definition_errors = self.validate(definition)

            if len(definition_errors) > 0:
                all_errors[name] = definition_errors

        return all_errors

    def validate(self, variable_definition):
        errors = []

        if not isinstance(variable_definition.get("required", None), bool):
            errors.append("Missing 'required' field.")

        if not isinstance(variable_definition.get("secret", None), bool):
            errors.append("Missing 'secret' field.")

        if not isinstance(variable_definition.get("type", None), str):
            errors.append("Missing 'type' field.")
            return errors

        type_specific_validators = {
            "float": self.validate_float_definition_type,
            "integer": self.validate_integer_definition_type,
            "string": self.validate_string_definition_type,
            "enum": self.validate_enum_definition_type,
        }

        validator = type_specific_validators.get(variable_definition["type"], None)

        if validator:
            validator(variable_definition, errors)
        else:
            errors.append("Unknown or missing 'type' field.")

        return errors

    def validate_float_definition_type(self, variable_definition, errors):
        pass

    def validate_integer_definition_type(self, variable_definition, errors):
        pass

    def validate_string_definition_type(self, variable_definition, errors):
        pass

    def validate_enum_definition_type(self, variable_definition, errors):
        if not isinstance(variable_definition.get("values", None), list):
            errors.append("Missing 'values' field for enum.")
