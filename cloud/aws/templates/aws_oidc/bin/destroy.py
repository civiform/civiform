#! /usr/bin/env python3
from cloud.aws.templates.aws_oidc.bin import resources
from cloud.aws.templates.aws_oidc.bin.aws_cli import AwsCli
from cloud.aws.templates.aws_oidc.bin.aws_template import AwsSetupTemplate
"""
Destroy the setup
"""


class Destroy(AwsSetupTemplate):

    def pre_terraform_destroy(self):
        if self.config.use_local_backend:
            self._make_backend_override()

    def post_terraform_destroy(self):
        # when config is dev then the state is stored locally and no clean up
        # required
        if not self.config.use_local_backend:
            print(
                'Not destroying S3 bucket that contains terraform state. ' +
                'You have to destroy it manually:')
            aws_cli = AwsCli(self.config)
            print(
                aws_cli.get_url_of_s3_bucket(
                    f'{self.config.app_prefix}-{resources.S3_TERRAFORM_STATE_BUCKET}'
                ))
