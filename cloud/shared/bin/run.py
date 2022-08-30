#! /usr/bin/env python3

import argparse
from asyncio import subprocess
import os
from cloud.shared.bin.lib.config_loader import ConfigLoader


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--image', required=True, help='Civiform image tag to be deployed.')
    parser.add_argument('--command', 'Command to run. If ommited will validate config and exit.')
    args=parser.parse_args()
    os.env['TF_VAR_image_tag']=args['image']

    config_loader = ConfigLoader()
    validation_errors = config_loader.load_config()
    if validation_errors:
        new_line = '\n\t'
        exit(
            f'Found the following validation errors: {new_line}{f"{new_line}".join(validation_errors)}'
        )
    
    if not os.path.exists(config_loader.get_template_dir):
        exit(f'Could not find template directory {config_loader.get_template_dir}')

    if args['command']:
        subprocess.check_call(shlex.split('python3'))


if __name__ == "__main__":
    main()