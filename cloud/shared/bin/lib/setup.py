#! /usr/bin/env python3

import os
import subprocess
import sys

from cloud.shared.bin.lib.config_loader import ConfigLoader
from cloud.shared.bin.lib.write_tfvars import TfVarWriter
from setup_class_loader import get_config_specific_setup
from cloud.shared.bin.lib import terraform
"""
Setup.py sets up and runs the initial terraform deployment. It's broken into
3 parts:
1) Load and Validate Inputs
2) Run Setup scripts
3) Terraform Init/Plan/Apply

The script generates a .tfvars file that is used to deploy via terraform.
"""


def main():
    ###############################################################################
    # Load and Validate Inputs
    ###############################################################################

    ## Load the Config and Definitions
    config_loader = ConfigLoader()

    validation_errors = config_loader.load_config()
    if validation_errors:
        new_line = '\n\t'
        exit(
            f"Found the following validation errors: {new_line}{f'{new_line}'.join(validation_errors)}"
        )

    ###############################################################################
    # Load Setup Class for the specific template directory
    ###############################################################################

    terraform_template_dir = config_loader.get_template_dir()
    template_setup = get_config_specific_setup(config_loader)

    template_setup.setup_log_file()
    current_user = template_setup.get_current_user()

    image_tag = config_loader.get_config_var("IMAGE_TAG")
    log_args = f"\"{image_tag}\" {current_user}"

    print("Writing TF Vars file")
    terraform_tfvars_path = os.path.join(
        terraform_template_dir, config_loader.tfvars_filename)

    # Write the passthrough vars to a temporary file
    tf_var_writter = TfVarWriter(terraform_tfvars_path)
    conf_variables = config_loader.get_terraform_variables()
    tf_var_writter.write_variables(conf_variables)

    try:
        print("Starting pre-terraform setup")
        template_setup.pre_terraform_setup()

        ###############################################################################
        # Terraform Init/Plan/Apply
        ###############################################################################
        print("Starting terraform deploy")
        terraform.perform_apply(config_loader)

        ###############################################################################
        # Post Run Setup Tasks (if needed)
        ###############################################################################
        if template_setup.requires_post_terraform_setup():
            print("Starting port-terraform setup")
            template_setup.post_terraform_setup()

            subprocess.check_call(
                [
                    "terraform", f"-chdir={terraform_template_dir}", "apply",
                    "-input=false", f"-var-file={config_loader.tfvars_filename}"
                ])

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
        print("Deployment Failed :(", file=sys.stderr)
        print("error:", err)

    finally:
        template_setup.cleanup()


if __name__ == "__main__":
    main()
