import os
import subprocess
import shlex
import shutil

from cloud.aws.templates.aws_oidc.bin.aws_template import AwsSetupTemplate
from cloud.aws.bin.lib import backend_setup


class Setup(AwsSetupTemplate):

    def get_current_user(self):
        get_current_command = "aws sts get-caller-identity --query UserId --output text"
        return subprocess.run(
            shlex.split(get_current_command), capture_output=True)

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
