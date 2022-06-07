#! /usr/bin/env python3

import os
import subprocess

"""
Destroy the setup
"""
class Destroy():
    def pre_terraform_destroy(self):
        print(" - Deleting Access Key")
        self._delete_access_key()

    def _delete_aws_access_key(self):
        access_key_id = os.getenv("AWS_ACCESS_KEY_ID")
        if access_key_id:
            subprocess.run([
                "aws",
                "iam",
                "delete-access-key",
                "--access-key-id", access_key_id
            ], check=True)
