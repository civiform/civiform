#! /usr/bin/env python3
import os
from cloud.shared.bin.lib.config_loader import ConfigLoader
from cloud.shared.bin.lib.write_tfvars import TfVarWriter
"""
This script generates a .tfvars file that is used to deploy via terraform.
"""

###############################################################################
# Load and Validate Inputs
###############################################################################

## Load the Config and Definitions


def load_config():
    config_loader = ConfigLoader()

    validation_errors = config_loader.load_config()
    if validation_errors:
        new_line = '\n\t'
        exit(
            f"Found the following validation errors: {new_line}{f'{new_line}'.join(validation_errors)}"
        )

    terraform_tfvars_filename = os.path.join(
        config_loader.get_template_dir(), "setup.auto.tfvars")

    # Write the passthrough vars to a temporary file
    tf_var_writter = TfVarWriter(terraform_tfvars_filename)
    tf_var_writter.write_variables(config_loader.get_terraform_variables())
    return config_loader
