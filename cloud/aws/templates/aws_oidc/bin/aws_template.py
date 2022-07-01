import os
import shutil

from cloud.aws.bin.lib import backend_setup
from cloud.shared.bin.lib import terraform
from cloud.shared.bin.lib.setup_template import SetupTemplate


class AwsSetupTemplate(SetupTemplate):

    def _tf_run_for_aws(self, is_destroy=False):
        template_dir = os.path.join(self.config.get_template_dir(), 'setup')
        print(" - Copy the tfvars file into the setup dir")
        shutil.copy2(
            os.path.join(
                self.config.get_template_dir(), self.config.tfvars_filename),
            os.path.join(
                self.config.get_template_dir(), 'setup',
                self.config.tfvars_filename))
        terraform.perform_apply(self.config, is_destroy, template_dir)

    def _setup_shared_state_file(self):
        if self.config.use_backend_config():
            backend_setup.setup_backend_config(self.config)
