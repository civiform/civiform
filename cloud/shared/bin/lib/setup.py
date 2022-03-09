import argparse
import subprocess

from config_loader import ConfigLoader
from write_tfvars import   TfVarWriter


"""
Setup.py sets up and runs the initial terraform deployment. It's broken into 
3 parts: 
1) Load and Validate Inputs
2) Run Setup scripts
3) Terraform Init/Plan/Apply

The script generates a .tfvars file that is used to deploy via terraform.
"""
parser = argparse.ArgumentParser()
parser.add_argument("-e", "--env", type=str, help="environment to setup")
parsed_args = parser.parse_args()

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
# Stack Specific Scripts
# TODO: add the specific palns here, eg things like below
# if config_loader.get_cloud_provider() == "azure":
#     subprocess.call("deploys/bin/azure/key-vault-setup")
#     subprocess.call("ssh-keygen -t rsa -b 4096 -f $HOME/.ssh/bastion", shell=True)
# if (
#     config_loader.get_email_sender == "SES"
#     and config_loader.get_cloud_provider() == "azure"
# ):
#     subprocess.call("deploys/bin/ses-to-keyvault")


###############################################################################
# Terraform Init/Plan/Apply
###############################################################################

terraform_directory = config_loader.get_template_dir()
terraform_tfvars_filename = f"{terraform_directory}/setup.auto.tfvars"

# Write the passthrough vars to a temporary file
tf_var_writter =   TfVarWriter(terraform_tfvars_filename)
variables_to_write = config_loader.get_terraform_variables()
tf_var_writter.write_variables(variables_to_write)

# TODO need to call input/apply like how the setup script in staging_azure
subprocess.call(
    f"terraform apply -input=false -var-file={terraform_tfvars_filename}",
    shell=True,
    cwd=terraform_directory,
)
