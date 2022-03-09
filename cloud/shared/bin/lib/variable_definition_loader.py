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

    def load_repo_variable_definitions_files(self):
        self.variable_definitions = {}
        cwd = os.getcwd()

        # As more variable definition files are added for each cloud provider,
        # add their paths here.
        definition_file_paths = [cwd + "/cloud/shared/variable_definitions.json"]

        for path in definition_file_paths:
            with open(path, "r") as file:
                definitions = json.loads(file.read())

                for name, definition in definitions.items():
                    if name in self.variable_definitions:
                        raise RuntimeError(f"Duplicate variable name: {name} at {path}")

                    self.variable_definitions[name] = definition

    def get_variable_definitions(self) -> dict:
        return self.variable_definitions
