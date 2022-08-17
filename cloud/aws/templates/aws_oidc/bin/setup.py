import secrets
from getpass import getpass
from typing import Dict

from cloud.aws.templates.aws_oidc.bin.aws_cli import AwsCli
from cloud.aws.templates.aws_oidc.bin import resources
from cloud.aws.templates.aws_oidc.bin.aws_template import AwsSetupTemplate
from cloud.aws.bin.lib import backend_setup
from cloud.shared.bin.lib.config_loader import ConfigLoader

# TODO(#3116): move these to variable_definitions.json and read docs from there.
# Map of secrets that need to be set by the user and can't be empty values.
# Key is the name of the secret without app prefix, value is doc shown to user
# if the secret is unset.
SECRETS: Dict[str, str] = {
    resources.ADFS_CLIENT_ID:
        'Client id for the ADFS configuration. Enter any value if you do not use ADFS.',
    resources.ADFS_SECRET:
        'Secret for the ADFS configuration. Enter any value if you do not use ADFS.',
    resources.APPLICANT_OIDC_CLIENT_ID:
        'Client ID for your OIDC provider. Enter any value if you have not set it up yet.',
    resources.APPLICANT_OIDC_CLIENT_SECRET:
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
        if self.config.use_local_backend:
            self._make_backend_override()

    def _setup_shared_state_file(self):
        if not self.config.use_local_backend:
            backend_setup.setup_backend_config(self.config)

    def requires_post_terraform_setup(self):
        return True

    def post_terraform_setup(self):
        if self.config.is_test():
            print(" - Test. Skipping post terraform setup.")
            return

        for name, doc in SECRETS.items():
            self._maybe_set_secret_value(
                f'{self.config.app_prefix}-{name}', doc)
        self._maybe_change_default_db_password()
        self._print_final_message()

    def _maybe_set_secret_value(self, secret_name: str, documentation: str):
        """
        Some secrets like login integration credentials created empty in
        terraform. The values need to be provided by users. This method runs
        after terraform created empty secrets and it asks user to provide
        actual secret values. Without these values server will not start so
        it has be run immediately after the initial setup is done.
        """
        print('')
        url = self._aws_cli.get_url_of_secret(secret_name)
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

    def _maybe_change_default_db_password(self):
        """
        Terraform creates database password secret with a random value and
        creates database and ECS service that use that password. The problem
        is that because password generated within terraform - its value is
        stored in the Terraform state. To avoid exposing password in the state
        this method regenerates password and updates database and server to use
        the new password.
        """
        print()
        print('Checking database password...')
        app_prefix = self.config.app_prefix
        secret_name = f'{app_prefix}-{resources.POSTGRES_PASSWORD}'
        if self._aws_cli.is_db_password_default(secret_name):
            new_password = secrets.token_urlsafe(40)
            print(
                'Default database password is used. Generating new password ' +
                'and updating deployment.')
            self._aws_cli.update_master_password_in_database(
                f'{app_prefix}-{resources.DATABASE}', new_password)
            print('Database password has been changed.')
            self._aws_cli.set_secret_value(secret_name, new_password)
            self._aws_cli.restart_ecs_service(
                f'{app_prefix}-{resources.CLUSTER}',
                f'{app_prefix}-{resources.FARGATE_SERVICE}')
            print(f'ECS service has been restarted to pickup the new password.')
        else:
            print('Password has already been changed. Not touching it.')
        print(
            f'You can see the password here: {self._aws_cli.get_url_of_secret(secret_name)}'
        )

    def _print_final_message(self):
        app = self.config.app_prefix

        # Print link to ECS tasks.
        print()
        fargate_service = f'{app}-{resources.FARGATE_SERVICE}'
        tasks_url = self._aws_cli.get_url_of_fargate_tasks(app, fargate_service)
        print('Setup finished. You can monitor civiform tasks status here:')
        print(tasks_url)

        # Print info about load balancer url.
        print()
        lb_dns = self._aws_cli.get_load_balancer_dns(
            f'{app}-{resources.LOAD_BALANCER}')
        print(f'Server is available on url: {lb_dns}')
        print('\nNext steps to complete your Civiform setup:')
        base_url = self.config.get_config_variables()['BASE_URL']
        print(
            f'In your domain registrar create a CNAME record for {base_url} to point to {lb_dns}.'
        )
        ses_address = self.config.get_config_variables()['SENDER_EMAIL_ADDRESS']
        print(
            f'Verify email address {ses_address}. If you didn\'t receive the ' +
            'confirmation email, check that your SES is not in sandbox mode.')
