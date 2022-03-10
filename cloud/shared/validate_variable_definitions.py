import os
import json

from bin.lib.variable_definition_loader import VariableDefinitionLoader

# Loads all configuration variable definition files and validates each
# definition for correctness. Exercised by the accompanying test file
# which is run for every pull request.
#
# Requires that:
#   - Each variable definition file is referenced in
#     def load_repo_variable_definitions_files():
#
#   - All variables have, at minimum, 'type', 'required', and 'secret' fields
#
#   - Variable definitions may include additional configuration based on their
#     type.
class ValidateVariableDefinitions:
    def __init__(self, variable_definitions={}):
        self.variable_definitions = variable_definitions

    def load_repo_variable_definitions_files(self):
        variable_def_loader = VariableDefinitionLoader(self.variable_definitions)
        # As more variable definition files are added for each cloud provider,
        # add their paths here.
        cwd = os.getcwd()
        definition_file_paths = [cwd + "/cloud/shared/variable_definitions.json"]

        for path in definition_file_paths:
            variable_def_loader.load_definition_file(path)
        self.variable_definitions = variable_def_loader.get_variable_definitions()

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
        
        if not isinstance(variable_definition.get("tfvar", None), bool):
            errors.append("Missing 'tfvar' field.")
            
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
