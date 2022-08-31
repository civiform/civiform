#! /usr/bin/env python3

import argparse
import os
import shlex
import subprocess

from cloud.shared.bin.lib.config_loader import ConfigLoader


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--image', required=True, help='Civiform image tag to be deployed.')
    parser.add_argument(
        '--command',
        help='Command to run. If ommited will validate config and exit.')
    args = parser.parse_args()
    os.environ['TF_VAR_image_tag'] = args.image

    config_loader = ConfigLoader()
    validation_errors = config_loader.load_config()
    if validation_errors:
        new_line = '\n\t'
        exit(
            f'Found the following validation errors: {new_line}{f"{new_line}".join(validation_errors)}'
        )

    if args.command:
        subprocess.check_call(shlex.split(f'python3 cloud/shared/bin/lib/{args.command}.py'))


if __name__ == "__main__":
    main()
