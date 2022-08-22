#! /usr/bin/env python3

from config_loader import ConfigLoader
from cloud.shared.bin.lib import terraform
from setup_class_loader import get_config_specific_destroy
"""
Destroy.py destroys Civiform deployment.
"""


def main():
    ###############################################################################
    # Load and Validate Inputs
    ###############################################################################

    # Load the Config and Definitions
    config_loader = ConfigLoader()

    if (config_loader.civiform_mode == "prod"):
        print(
            'Destoy is not allowed in PROD mode. Use AWS console if you need to '
            + 'destroy your production version of Civiform.')
        return

    validation_errors = config_loader.load_config()
    if validation_errors:
        new_line = '\n\t'
        exit(
            f'Found the following validation errors: {new_line}{f"{new_line}".join(validation_errors)}'
        )

    ###############################################################################
    # Load Setup Class for the specific template directory
    ###############################################################################

    template_destroy = get_config_specific_destroy(config_loader)
    template_destroy.pre_terraform_destroy()
    terraform.perform_apply(config_loader, is_destroy=True)
    template_destroy.post_terraform_destroy()


if __name__ == '__main__':
    main()
