import subprocess
import os
import shutil

from cloud.shared.bin.lib import civiform_mode


def perform_apply():
    '''Generates terraform variable files and runs terraform init and apply.'''

    TERRAFORM_PLAN_OUT_FILE = "terraform_plan"
    TERRAFORM_TEMPLATE_DIR = os.getenv("TERRAFORM_TEMPLATE_DIR")
    TF_VARS_FILENAME = os.getenv('TF_VAR_FILENAME')

    terraform_cmd = ['terraform', f'-chdir={TERRAFORM_TEMPLATE_DIR}']

    if civiform_mode.is_dev():
        subprocess.check_call(
            terraform_cmd + ['init', '-upgrade', '-reconfigure'])
    else:
        subprocess.check_call(
            terraform_cmd + [
                'init', '-input=false', '-upgrade',
                f'-backend-config={os.getenv("BACKEND_VARS_FILENAME")}'
            ])

    if os.path.exists(os.path.join(TERRAFORM_TEMPLATE_DIR, TF_VARS_FILENAME)):
        print(
            f'{TF_VARS_FILENAME} exists in {TERRAFORM_TEMPLATE_DIR} directory')
    else:
        print(
            f'Aborting the script. {TF_VARS_FILENAME} does not exists in {TERRAFORM_TEMPLATE_DIR} directory'
        )
        raise 1

    subprocess.check_call(
        terraform_cmd + [
            'plan', '-input=false', f'-out={TERRAFORM_PLAN_OUT_FILE}',
            f'-var-file={os.getenv("TF_VAR_FILENAME")}'
        ])

    if civiform_mode.is_test():
        return True

    terraform_apply_cmd = terraform_cmd + ['apply', '-input=false', '-json']
    if not civiform_mode.is_dev():
        subprocess.check_call(
            terraform_apply_cmd +
            ['-auto-approve', f'{TERRAFORM_PLAN_OUT_FILE}'])
    else:
        subprocess.check_call(
            terraform_apply_cmd + [f'{TERRAFORM_PLAN_OUT_FILE}'])

    return True


def copy_backend_override():
    ''' 
  Copies the terraform backend_override to backend_override.tf (used to
  make backend local instead of a shared state for dev deploys)
  '''
    backend_override_path = os.path.join(
        os.getenv('TERRAFORM_TEMPLATE_DIR'), 'backend_override')
    if not os.path.exists(backend_override_path):
        print(f'{backend_override_path} does not exist.')
        return

    shutil.copy(
        backend_override_path,
        os.path.join(
            os.getenv('TERRAFORM_TEMPLATE_DIR'), 'backend_override.tf'))
