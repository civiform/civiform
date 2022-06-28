import os
import subprocess
import shlex
import shutil

from cloud.shared.bin.lib.setup_template import SetupTemplate
from cloud.aws.bin.lib import backend_setup


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
        print(" - Run terraform init")
        subprocess.check_call(
            [
                "terraform",
                f"-chdir={template_dir}",
                "init",
                "-input=false",
                "-upgrade",
            ])

        tf_apply_args = [
            "terraform", f"-chdir={template_dir}", "apply", "-input=false"
        ]
        
        if is_destroy:
            tf_apply_args.append("-destroy")

        if not self.config.is_dev():
            tf_apply_args.append("-auto-approve")

        print(" - Run terraform apply")
        subprocess.check_call(tf_apply_args)

    def _setup_shared_state_file(self):
        if self.config.use_backend_config():
            backend_setup.setup_backend_config(self.config)
