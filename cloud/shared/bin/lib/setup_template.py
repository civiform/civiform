#! /usr/bin/env python3

import tempfile
"""
Template Setup

These functions need to be defined for every template.
"""


class SetupTemplate:

    log_file_path = None

    def __init__(self, config):
        self.config = config

    def pre_terraform_setup(self):
        print(" - TODO: Pre terraform setup.")

    def get_current_user(self):
        print(" - TODO: Get Current user.")

    def setup_log_file(self):
        _, self.log_file_path = tempfile.mkstemp()
        print(" - TODO: Setup log file here.")

    def requires_post_terraform_setup(self):
        return False

    def post_terraform_setup(self):
        print(" - TODO: Post terraform setup.")

    def cleanup(self):
        print(" - TODO: cleanup. Upload log files.")
