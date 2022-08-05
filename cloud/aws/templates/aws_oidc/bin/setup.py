import json
import os
import subprocess
import shlex
import shutil

from cloud.aws.templates.aws_oidc.bin.aws_template import AwsSetupTemplate
from cloud.aws.bin.lib import backend_setup


class Setup(AwsSetupTemplate):

    def get_current_user(self):
        get_current_command = "aws sts get-caller-identity --query UserId --output text"
        current_user_process = subprocess.run(
            shlex.split(get_current_command), capture_output=True)
        current_user = current_user_process.stdout.decode("ascii")
        if not current_user:
            raise RuntimeError("Could not find the logged in user")
        return current_user

    def pre_terraform_setup(self):
        print(" - Running the setup script in terraform")
        self._tf_run_for_aws(is_destroy=False)
        print(" - Setting up shared state file")
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

        out = subprocess.check_output(['aws', 'secretsmanager', 'list-secrets', '--output=json', f'--region={self.config.aws_region}'])
        secrets = json.loads(out.decode("ascii"))
        print(self._get_secret_by_name(secrets, f'{self.config.app_prefix}-adfs_client_id'))

    def _get_secret_by_name(self, secrets: dict, name: str) -> dict:
        for secret in secrets['SecretList']:
            if secret['Name'] == name:
                return secret
        raise ValueError(f'Secret with name {name} is not found. Was it succesfully created?')
