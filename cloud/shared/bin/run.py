#! /usr/bin/env python3

import argparse
from distutils.cmd import Command
import os
import sys
import importlib

sys.path.append(os.getcwd())

from cloud.shared.bin.lib.config_loader import ConfigLoader


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--image', help='Civiform image tag. Required for Setup and Deploy.')
    parser.add_argument(
        '--command',
        help='Command to run. If ommited will validate config and exit.')
    args = parser.parse_args()
    if args.image:
        os.environ['TF_VAR_image_tag'] = args.image
    elif args.command is not 'destroy':
        exit('--image is required')

    config = ConfigLoader()
    validation_errors = config.load_config()
    if validation_errors:
        new_line = '\n\t'
        exit(
            f'Found the following validation errors: {new_line}{f"{new_line}".join(validation_errors)}'
        )

    if args.command:
        if not os.path.exists(f'cloud/shared/bin/lib/{args.command}.py'):
            exit(f'Command {args.command} not found.')
        command_module = importlib.import_module(
            f'cloud.shared.bin.{args.command}')
        if not command_module:
            exit(f'Command {args.command} not found.')
        command_module.run(config)


if __name__ == "__main__":
    main()
