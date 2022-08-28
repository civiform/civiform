#! /usr/bin/env python3
import sys

from cloud.aws.templates.aws_oidc.bin import resources
from cloud.aws.templates.aws_oidc.bin.aws_cli import AwsCli
from cloud.shared.bin.lib import terraform
from cloud.shared.bin.lib import tf_apply_setup
from cloud.aws.bin.lib import backend_setup


def main():
    config_loader = tf_apply_setup.load_config()

    if config_loader.use_local_backend:
        terraform.copy_backend_override(config_loader)
    else:
        backend_setup.setup_backend_config(config_loader)

    if not terraform.perform_apply(config_loader):
        sys.stderr.write('Terraform deployment failed.')
        # TODO(#2606): write and upload logs.
        raise ValueError('Terraform deployment failed.')

    if config_loader.is_test():
        print('Test completed')

    print()
    print('Deployment finished. You can monitor civiform tasks status here:')
    print(
        AwsCli(config_loader).get_url_of_fargate_tasks(
            f'{config_loader.app_prefix}-{resources.CLUSTER}',
            f'{config_loader.app_prefix}-{resources.FARGATE_SERVICE}'))


if __name__ == "__main__":
    main()
