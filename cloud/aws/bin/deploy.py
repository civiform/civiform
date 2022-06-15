#! /usr/bin/env python3
import subprocess
import os
import sys

sys.path.append(os.getcwd())

from cloud.shared.bin.lib import civiform_mode
from cloud.shared.bin.lib import terraform

# Keep in sync with cloud/shared/bin/lib.sh
os.environ['TF_VAR_FILENAME'] = 'setup.auto.tfvars'
os.environ['BACKEND_VARS_FILENAME'] = 'backend_vars'

subprocess.check_call(['cloud/shared/bin/lib/tf_apply_setup.py'])

if civiform_mode.is_dev():
    terraform.copy_backend_override()

if not terraform.perform_apply():
    print('Terraform deployment failed.')
    # TODO: write and upload logs.
    raise 1

if civiform_mode.is_test():
    print('Test completed')
