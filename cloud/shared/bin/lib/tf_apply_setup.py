#! /usr/bin/env python3

from config_loader import ConfigLoader
from write_tfvars import TfVarWriter

"""
This script generates a .tfvars file that is used to deploy via terraform.
"""

###############################################################################
# Load and Validate Inputs
###############################################################################

## Load the Config and Definitions
config_loader = ConfigLoader()

is_valid, validation_errors = config_loader.load_config()
if not is_valid:
    new_line = '\n\t'
    exit(f"Found the following validation errors: {new_line}{f'{new_line}'.join(validation_errors)}")

template_dir = config_loader.get_template_dir()

terraform_tfvars_filename = f"{template_dir}/setup.auto.tfvars"

# Write the passthrough vars to a temporary file
tf_var_writter = TfVarWriter(terraform_tfvars_filename)
variables_to_write = config_loader.get_terraform_variables()
tf_var_writter.write_variables(variables_to_write)
