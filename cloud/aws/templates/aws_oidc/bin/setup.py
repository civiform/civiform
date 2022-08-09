import subprocess
import shlex
from getpass import getpass
from typing import Dict

from cloud.aws.templates.aws_oidc.bin.aws_cli import AwsCli
from cloud.aws.templates.aws_oidc.bin.aws_template import AwsSetupTemplate
from cloud.aws.bin.lib import backend_setup
from cloud.shared.bin.lib.config_loader import ConfigLoader

# TODO(#3116): move these to variable_definitions.json and read docs from there.
# Map of secrets that need to be set by the user and can't be empty values.
# Key is the name of the secret without app prefix, value is doc shown to user
# if the secret is unset.
SECRETS: Dict[str, str] = {
    'adfs_client_id':
        'Client id for the ADFS configuration. Enter any value if you do not use ADFS.',
    'adfs_secret':
        'Secret for the ADFS configuration. Enter any value if you do not use ADFS.',
    'applicant_oidc_client_id':
        'Client ID for your OIDC provider. Enter any value if you have not set it up yet.',
    'applicant_oidc_client_secret':
        'Client secret for your OIDC provider. Enter any value if you have not set it up yet.',
}


class Setup(AwsSetupTemplate):

    def __init__(self, config: ConfigLoader):
        super().__init__(config)
        self._aws_cli = AwsCli(config)

    def get_current_user(self) -> str:
        current_user = self._aws_cli.get_current_user()
        if not current_user:
            raise RuntimeError('Could not find the logged in user')
        return current_user

    def pre_terraform_setup(self):
        print(' - Running the setup script in terraform')
        self._tf_run_for_aws(is_destroy=False)
        print(' - Setting up shared state file')
        self._setup_shared_state_file()
        # Only run in dev mode
        if not self.config.use_backend_config():
            self._make_backend_override()

    def _setup_shared_state_file(self):
        if self.config.use_backend_config():
            backend_setup.setup_backend_config(self.config)

    def requires_post_terraform_setup(self):
        return True

    def post_terraform_setup(self):
        for name, doc in SECRETS.items():
            self._maybe_set_secret_value(
                f'{self.config.app_prefix}-{name}', doc)

    def _maybe_set_secret_value(self, secret_name: str, documentation: str):
        print('')
        url = f'https://{self.config.aws_region}.console.aws.amazon.com/secretsmanager/secret?name={secret_name}'
        if self._aws_cli.is_secret_empty(secret_name):
            print(
                f'Secret {secret_name} is not set. It needs to be set to a non-empty value.'
            )
            print(documentation)
            print(f'You can later change the value in AWS console: {url}')
            new_value = getpass('enter value -> ').strip()
            while new_value.strip() == '':
                print('Value cannot be empty.')
                new_value = getpass('enter value -> ').strip()
            self._aws_cli.set_secret_value(secret_name, new_value)
            print('Secret value successfully set.')
        else:
            print(f'Secret {secret_name} already has a value set.')
            print(f'You can check and update it in AWS console: {url}')
