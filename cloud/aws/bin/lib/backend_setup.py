import os


def setup_backend_config(config):
    backend_file_location = os.path.join(
        config.get_template_dir(), config.backend_vars_filename)
    with open(backend_file_location, 'w') as f:
        f.write(f'bucket         = "{config.app_prefix}-backendstate"\n')
        f.write(f'key            = "tfstate/terraform.tfstate"\n')
        f.write(f'region         = "{config.aws_region}"\n')
        f.write(f'dynamodb_table = "{config.app_prefix}-locktable"\n')
        f.write(f'encrypt        = true\n')
