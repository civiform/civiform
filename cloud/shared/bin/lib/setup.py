#! /usr/bin/env python3

import subprocess

from config_loader import ConfigLoader
from write_tfvars import TfVarWriter
from setup_class_loader import load_class

"""
Setup.py sets up and runs the initial terraform deployment. It's broken into 
3 parts: 
1) Load and Validate Inputs
2) Run Setup scripts
3) Terraform Init/Plan/Apply

The script generates a .tfvars file that is used to deploy via terraform.
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

###############################################################################
# Load Setup Class for the specific template directory
###############################################################################

template_dir = config_loader.get_template_dir()
Setup = load_class(template_dir)
template_setup = Setup()

###############################################################################
# Terraform Init/Plan/Apply
###############################################################################

terraform_tfvars_filename = f"{template_dir}/setup.auto.tfvars"

# Write the passthrough vars to a temporary file
tf_var_writter = TfVarWriter(terraform_tfvars_filename)
variables_to_write = config_loader.get_terraform_variables()
tf_var_writter.write_variables(variables_to_write)

# TODO need to call input/apply like how the setup script in staging_azure
subprocess.call(
    f"terraform apply -input=false -var-file={terraform_tfvars_filename}",
    shell=True,
    cwd=template_dir,
)
