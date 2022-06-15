#! /usr/bin/env python3
import subprocess
import os
import sys

# TODO(#2743): move this to deploy specific script.
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
    sys.stderr.write('Terraform deployment failed.')
    # TODO(#2606): write and upload logs.
    raise ValueError('Terraform deployment failed.')

if civiform_mode.is_test():
    print('Test completed')
