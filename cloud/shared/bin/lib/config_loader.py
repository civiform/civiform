import subprocess
import shlex
import os
import re

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

    @property
    def aws_region(self):
        # NOTE: should we make AWS_REGION required and avoid having a default
        # value here?
        return os.environ.get('AWS_REGION', 'us-east-1')

    @property
    def civiform_mode(self):
        return os.environ['CIVIFORM_MODE']

    @property
    def use_local_backend(self):
        return os.getenv('USE_LOCAL_BACKEND', False)

    @property
    def skip_confirmations(self):
        return os.getenv('SKIP_CONFIRMATIONS', False)

    def load_config(self, config_file):
        self._load_config(config_file)
        return self.validate_config()

    def _get_config_values_from_sh_file(self, config_file):
        ## 1. Export all variables from the config into clean environment
        ## 2. Set values in current environment
        if not os.path.exists(config_file):
            exit(f'Cannot find file {config_file}')
        print(f'Getting config from {config_file}')
        command = shlex.split(f'env -i bash -c "source {config_file} && env"')
        proc = subprocess.Popen(command, stdout=subprocess.PIPE)
        for line in proc.stdout:
            (key, _, value) = line.decode().partition("=")
            os.environ[key] = value.strip()
        proc.communicate()

    def _load_config(self, config_file):
        self._get_config_values_from_sh_file(config_file)

        # get the shared variable definitions
        variable_def_loader = VariableDefinitionLoader()
        cwd = os.getcwd()
        definition_file_path = os.path.join(
            cwd, 'cloud', 'shared', 'variable_definitions.json')
        variable_def_loader.load_definition_file(definition_file_path)
        shared_definitions = variable_def_loader.get_variable_definitions()
        self.configs = self.get_env_variables(shared_definitions)

        template_definitions_file_path = os.path.join(
            self.get_template_dir(), 'variable_definitions.json')
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

    def _validate_config(self, variable_definitions: dict, configs: dict):
        validation_errors = []

        for name, definition in variable_definitions.items():
            is_required = definition.get('required', False)
            config_value = configs.get(name, None)

            if config_value is None:
                if is_required:
                    validation_errors.append(
                        f'[{name}] required, but not provided')
                continue

            is_enum = definition.get('type') == 'enum'

            if is_enum:
                if config_value not in definition.get('values'):
                    validation_errors.append(
                        f'[{name}] \'{config_value}\' is not a supported enum value. Want a value in [{", ".join(definition.get("values"))}]'
                    )

            value_regex = definition.get('value_regex', None)
            if value_regex:
                # TODO(#2887): If we validate variable definitions prior to
                # trying to validate an actual configuration, we can assume that
                # this will always be set if value_regex is provided.
                validation_error_message = definition.get(
                    'value_regex_error_message', None)
                if not validation_error_message:
                    raise ValueError(
                        f'[{name}] no value_regex_error_message configured')
                if not re.compile(value_regex).fullmatch(config_value):
                    validation_errors.append(
                        f'[{name}] {validation_error_message}')

        return validation_errors

    def validate_config(self):
        return self._validate_config(self.variable_definitions, self.configs)

    def get_config_var(self, variable_name):
        return self.configs.get(variable_name)

    def get_cloud_provider(self):
        return self.configs.get('CIVIFORM_CLOUD_PROVIDER')

    def get_template_dir(self):
        template_dir = self.configs.get('TERRAFORM_TEMPLATE_DIR')
        if template_dir is None or not os.path.exists(template_dir):
            exit(f'Could not find template directory {template_dir}')
        return template_dir

    def is_test(self):
        return self.civiform_mode == 'test'

    def get_config_variables(self):
        return self.configs

    def _get_terraform_variables(
            self, variable_definitions: dict, configs: dict):
        tf_variables = list(
            filter(
                lambda x: variable_definitions.get(x).get('tfvar'),
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
