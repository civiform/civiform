import os
import subprocess
import sys

from cloud.shared.bin.lib.config_loader import ConfigLoader
from cloud.shared.bin.lib.setup_class_loader import get_config_specific_setup
from cloud.shared.bin.lib import terraform
"""
Setup.py sets up and runs the initial terraform deployment. It's broken into
2 parts:
1) Run Setup scripts
2) Terraform Init/Plan/Apply

The script generates a .tfvars file that is used to deploy via terraform.
"""


def run(config: ConfigLoader):
    ###############################################################################
    # Load Setup Class for the specific template directory
    ###############################################################################

    template_setup = get_config_specific_setup(config)

    template_setup.setup_log_file()
    current_user = template_setup.get_current_user()

    image_tag = config.get_config_var("IMAGE_TAG")
    log_args = f"\"{image_tag}\" {current_user}"

    try:
        print("Starting pre-terraform setup")
        template_setup.pre_terraform_setup()

        ###############################################################################
        # Terraform Init/Plan/Apply
        ###############################################################################
        print("Starting terraform deploy")
        terraform.perform_apply(config)

        ###############################################################################
        # Post Run Setup Tasks (if needed)
        ###############################################################################
        if template_setup.requires_post_terraform_setup():
            print("Starting port-terraform setup")
            template_setup.post_terraform_setup()

        subprocess.run(
            [
                "/bin/bash", "-c",
                f"source cloud/shared/bin/lib.sh && LOG_TEMPFILE={template_setup.log_file_path} log::deploy_succeeded {log_args}"
            ],
            check=True)
    except BaseException as err:
        subprocess.run(
            [
                "/bin/bash", "-c",
                f"source cloud/shared/bin/lib.sh && LOG_TEMPFILE={template_setup.log_file_path} log::deploy_failed {log_args}"
            ],
            check=True)
        print(
            "\nDeployment Failed. Check Troubleshooting page for known issues:\n"
            +
            "https://docs.civiform.us/it-manual/sre-playbook/terraform-deploy-system#troubleshooting\n",
            file=sys.stderr)
        # rethrow error so that full stack trace is printed
        raise err

    finally:
        template_setup.cleanup()
