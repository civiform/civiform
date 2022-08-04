#! /usr/bin/env python3

from config_loader import ConfigLoader
from cloud.shared.bin.lib import terraform
from setup_class_loader import load_destroy_class
"""
Destroy.py destroys the setup
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

    template_dir = config_loader.get_template_dir()
    Destroy = load_destroy_class(template_dir)

    template_destroy = Destroy(config_loader)
    template_destroy.pre_terraform_destroy()
    terraform.perform_apply(config_loader, is_destroy=True)
    template_destroy.post_terraform_destroy()


if __name__ == "__main__":
    main()
