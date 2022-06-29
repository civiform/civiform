#! /usr/bin/env python3

import os
import subprocess
import shutil

from cloud.aws.templates.aws_oidc.bin.aws_template import AwsSetupTemplate
"""
Destroy the setup
"""


class Destroy(AwsSetupTemplate):

    def pre_terraform_destroy(self):
        if not self.config.use_backend_config():
            self._make_backend_override()

    def post_terraform_destroy(self):
        self._tf_run_for_aws(is_destroy=True)
