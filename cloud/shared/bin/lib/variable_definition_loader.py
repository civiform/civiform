import os
import json

# Loads all configuration variable definition files
#
# Requires that:
#   - Each variable definition file is referenced in
#     def load_repo_variable_definitions_files():
class VariableDefinitionLoader:
    def __init__(self, variable_definitions={}):
        self.variable_definitions: dict = variable_definitions

    def load_definition_file(self, definition_file_path: str):
        with open(definition_file_path, "r") as file:
            definitions = json.loads(file.read())

            for name, definition in definitions.items():
                if name in self.variable_definitions:
                    raise RuntimeError(f"Duplicate variable name: {name} at {definition_file_path}")

                self.variable_definitions[name] = definition

    def get_variable_definitions(self) -> dict:
        return self.variable_definitions
