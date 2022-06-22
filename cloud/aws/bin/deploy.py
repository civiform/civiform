#! /usr/bin/env python3
import subprocess
import os
import sys

from cloud.shared.bin.lib import terraform
from cloud.shared.bin.lib import tf_apply_setup

config_loader = tf_apply_setup.load_config()

if config_loader.is_dev():
    terraform.copy_backend_override(config_loader)

if not terraform.perform_apply(config_loader):
    sys.stderr.write('Terraform deployment failed.')
    # TODO(#2606): write and upload logs.
    raise ValueError('Terraform deployment failed.')

if config_loader.is_test():
    print('Test completed')
