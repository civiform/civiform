#! /usr/bin/env python3

"""
Template Setup

These functions need to be defined for every template.
"""
class SetupTemplate:

    def __init__(self, config):
        self.config=config


    def pre_terraform_setup(self):
        print(" - TODO: Pre terraform setup.")


    def setup_log_file(self):
       print(" - TODO: Setup log file here.")

    def requires_post_terraform_setup(self):
        return False

    def post_terraform_setup(self):
        print(" - TODO: Post terraform setup.")
