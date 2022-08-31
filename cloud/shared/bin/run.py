#! /usr/bin/env python3

import argparse
import os
import shlex
import subprocess
import importlib

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

    config = ConfigLoader()
    validation_errors = config.load_config()
    # if validation_errors:
    #     new_line = '\n\t'
    #     exit(
    #         f'Found the following validation errors: {new_line}{f"{new_line}".join(validation_errors)}'
    #     )

    if args.command:
        if not os.path.exists(f'cloud/shared/bin/lib/{args.command}.py'):
            exit(f'Command {args.command} not found.')
        command_module = importlib.import_module(f'cloud.shared.bin.lib.{args.command}')
        if not command_module:
            exit(f'Command {args.command} not found.')
        command_module.main(config)


if __name__ == "__main__":
    main()
