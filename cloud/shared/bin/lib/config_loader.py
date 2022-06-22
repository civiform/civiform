import os

from cloud.shared.bin.lib.variable_definition_loader import VariableDefinitionLoader
"""
Config Loader
  Handles validating and getting data from the configuration/variable files
  
  Call load_config to get the variable definitions and corresponding env
  variables. Will return if the config is valid and the validation errors.
  
  Provides getters to return values from the config. 
"""


class ConfigLoader:

    @property
    def tfvars_filename(self):
        return os.environ['TF_VAR_FILENAME']

    @property
    def backend_vars_filename(self):
        return os.environ['BACKEND_VARS_FILENAME']

    @property
    def app_prefix(self):
        return os.environ['APP_PREFIX']

    def load_config(self):
        self._load_config()
        return self.validate_config()

    def _load_config(self):
        # get the shared variable definitions
        variable_def_loader = VariableDefinitionLoader()
        cwd = os.getcwd()
        definition_file_path = f'{cwd}/cloud/shared/variable_definitions.json'
        variable_def_loader.load_definition_file(definition_file_path)
        shared_definitions = variable_def_loader.get_variable_definitions()
        self.configs = self.get_env_variables(shared_definitions)

        template_definitions_file_path = f'{self.get_template_dir()}/variable_definitions.json'
        variable_def_loader.load_definition_file(template_definitions_file_path)
        self.variable_definitions = variable_def_loader.get_variable_definitions(
        )
        self.configs = self.get_env_variables(self.variable_definitions)

    def get_shared_variable_definitions(self):
        variable_def_loader = VariableDefinitionLoader()
        variable_def_loader.load_repo_variable_definitions_files()
        return variable_def_loader.get_variable_definitions()

    def get_env_variables(self, variable_definitions: dict):
        configs: dict = {}
        for name in variable_definitions.keys():
            configs[name] = os.environ.get(name, None)
        return configs

    # TODO: we do not validate type of the variable as we only have
    # strings currently. If we add non-strings, will need to validate
    def _validate_config(self, variable_definitions: dict, configs: dict):
        is_valid = True
        validation_errors = []

        for name, definition in variable_definitions.items():
            is_required = definition.get("required", False)
            config_value = configs.get(name, None)

            if is_required and config_value is None:
                is_valid = False
                validation_errors.append(
                    f"{name} is required, but not provided")

            is_enum = definition.get("type") == "enum"

            if config_value is not None and is_enum:
                if config_value not in definition.get("values"):
                    is_valid = False
                    validation_errors.append(
                        f"{config_value} not supported enum for {name}")

        return is_valid, validation_errors

    def validate_config(self):
        return self._validate_config(self.variable_definitions, self.configs)

    def get_config_var(self, variable_name):
        return self.configs.get(variable_name)

    def get_cloud_provider(self):
        return self.configs.get("CIVIFORM_CLOUD_PROVIDER")

    def get_template_dir(self):
        return self.configs.get("TERRAFORM_TEMPLATE_DIR")

    def is_dev(self):
        civiform_mode = self.configs.get("CIVIFORM_MODE")
        return civiform_mode == "dev"

    def is_test(self):
        civiform_mode = self.configs.get("CIVIFORM_MODE")
        return civiform_mode == "test"

    def use_backend_config(self):
        return not self.is_dev()

    def get_config_variables(self):
        return self.configs

    def _get_terraform_variables(
            self, variable_definitions: dict, configs: dict):
        tf_variables = list(
            filter(
                lambda x: variable_definitions.get(x).get("tfvar"),
                self.variable_definitions,
            ))
        tf_config_vars = {}
        for key, value in configs.items():
            if key in tf_variables:
                tf_config_vars[key] = value
        return tf_config_vars

    def get_terraform_variables(self):
        return self._get_terraform_variables(
            self.variable_definitions, self.configs)
