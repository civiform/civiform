#! /usr/bin/env python3

from distutils.command.config import config
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
backend_vars = "backend_vars" if config_loader.use_backend_config() else None 
template_setup = Setup(config_loader, backend_vars)
template_setup.pre_terraform_setup()

###############################################################################
# Terraform Init/Plan/Apply
###############################################################################

terraform_tfvars_filename = "setup.auto.tfvars"
terraform_tfvars_path = f"{template_dir}/{terraform_tfvars_filename}"

# Write the passthrough vars to a temporary file
tf_var_writter = TfVarWriter(terraform_tfvars_path)
conf_variables = config_loader.get_terraform_variables()
tf_var_writter.write_variables(conf_variables)

# Note that the -chdir means we use the relative paths for 
# both the backend config and the var file
terraform_init_args = [
    "terraform", 
    f"-chdir={template_dir}", 
    "init", 
]
if backend_vars:
    terraform_init_args.append(f"-backend-config={backend_vars}")

subprocess.check_call(terraform_init_args)

subprocess.check_call([
    "terraform", 
    f"-chdir={template_dir}", 
    "apply", 
    f"-var-file={terraform_tfvars_filename}"
])

###############################################################################
# Post Run Setup Tasks (if needed)
###############################################################################

if template_setup.requires_post_terraform_setup():
    template_setup.post_terraform_setup()
    
    subprocess.check_call([
        "terraform", 
        f"-chdir={template_dir}", 
        "apply", 
        f"-var-file={terraform_tfvars_filename}"
    ])

template_setup.cleanup()
