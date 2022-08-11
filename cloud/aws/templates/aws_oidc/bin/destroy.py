#! /usr/bin/env python3

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
            print(
                f'https://s3.console.aws.amazon.com/s3/buckets/{self.config.app_prefix}-backendstate'
            )
