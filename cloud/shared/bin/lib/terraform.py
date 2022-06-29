import subprocess
import os
import shutil
import shlex


# TODO(#2741): When using this for Azure make sure to setup backend bucket prior to calling these functions.
def perform_apply(config_loader, is_destroy=False, terraform_template_dir=None):
    '''Generates terraform variable files and runs terraform init and apply.'''
    if not terraform_template_dir:
        terraform_template_dir = config_loader.get_template_dir()
    tf_vars_filename = config_loader.tfvars_filename

    terraform_cmd = f'terraform -chdir={terraform_template_dir}'

    if config_loader.is_dev():
        subprocess.check_call(
            shlex.split(f'{terraform_cmd} init -upgrade -reconfigure'))
    else:
        subprocess.check_call(
            shlex.split(
                f'{terraform_cmd} init -input=false -upgrade -backend-config={os.getenv("BACKEND_VARS_FILENAME")}'
            ))

    if os.path.exists(os.path.join(terraform_template_dir, tf_vars_filename)):
        print(
            f'{tf_vars_filename} exists in {terraform_template_dir} directory')
    else:
        raise ValueError(
            f'Aborting the script. {tf_vars_filename} does not exist in {terraform_template_dir} directory'
        )

    terraform_plan_out_file = 'terraform_plan'
    plan_arguments = f'{terraform_cmd} plan -input=false -out={terraform_plan_out_file} -var-file={tf_vars_filename}'
    if is_destroy:
        plan_arguments += " -destroy"
    subprocess.check_call(shlex.split(plan_arguments))

    if config_loader.is_test():
        return True

    terraform_apply_cmd = f'{terraform_cmd} apply -input=false -json'
    if config_loader.is_dev():
        subprocess.check_call(
            shlex.split(f'{terraform_apply_cmd} {terraform_plan_out_file}'))
    else:
        subprocess.check_call(
            shlex.split(
                f'{terraform_apply_cmd} -auto-approve {terraform_plan_out_file}'
            ))

    return True


def copy_backend_override(config_loader):
    ''' 
    Copies the terraform backend_override to backend_override.tf (used to
    make backend local instead of a shared state for dev deploys)
    '''
    backend_override_path = os.path.join(
        config_loader.get_template_dir(), 'backend_override')
    if not os.path.exists(backend_override_path):
        print(f'{backend_override_path} does not exist.')
        return

    shutil.copy(
        backend_override_path,
        os.path.join(config_loader.get_template_dir(), 'backend_override.tf'))
