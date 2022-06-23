import os
import subprocess
import shlex
import shutil

from cloud.shared.bin.lib.setup_template import SetupTemplate


class Setup(SetupTemplate):

    def get_current_user(self):
        get_current_command = "aws sts get-caller-identity --query UserId --output text"
        return subprocess.run(
            shlex.split(get_current_command), capture_output=True)

    def pre_terraform_setup(self):
        print(" - Running the setup script in terraform")
        self._run_tf_to_setup()
        print(" - Setting up shared state file")
        self._setup_shared_state_file()
        # Only run in dev mode
        if not self.config.use_backend_config():
            self._make_backend_override()

    def _run_tf_to_setup(self):
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

        if not self.config.is_dev():
            tf_apply_args.append("-auto-approve")

        print(" - Run terraform apply")
        subprocess.check_call(tf_apply_args)

    def _setup_shared_state_file(self):
        if self.config.use_backend_config():
            backend_file_location = os.path.join(
                self.config.get_template_dir(),
                self.config.backend_vars_filename)
            with open(backend_file_location, 'w') as f:
                f.write(
                    f'bucket         = "${self.config.app_prefix}-backendstate"\n'
                )
                f.write(f'key            = "tfstate/terraform.tfstate"\n')
                f.write(f'region         = "us-east-1"\n')
                f.write(
                    f'dynamodb_table = "${self.config.app_prefix}-locktable"\n')
                f.write(f'encrypt        = true\n')
