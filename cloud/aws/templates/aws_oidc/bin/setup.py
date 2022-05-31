import shutil
import subprocess
import tempfile

"""
Template Setup
This script handles the setup for the specific template. Calls out
to many different shell script in order to setup the environment
outside of the terraform setup. The setup is in two phases: pre_terraform_setup
and post_terraform_setup.
"""
class Setup:

    def __init__(self, config):
         self.config=config

    def requires_post_terraform_setup(self):
        return False

    def pre_terraform_setup(self):
        print(" - Pre terraform setup. Doing nothing for now.")
        

    def setup_log_file(self):
        print(" - TODO: setup log file")

    def post_terraform_setup(self):
        print(" - Post terraform setup. Doing nothing for now.")

    def cleanup(self):
        print(" - Cleanup. TODO: Upload log file here.")
