"""
Template Setup

These functions need to be defined for every template.
"""

import tempfile
import shutil

from cloud.shared.bin.lib.config_loader import ConfigLoader


class SetupTemplate:

    log_file_path = None

    def __init__(self, config: ConfigLoader):
        self.config: ConfigLoader = config

    def _make_backend_override(self):
        current_directory = self.config.get_template_dir()
        shutil.copy2(
            f'{current_directory}/backend_override',
            f'{current_directory}/backend_override.tf')

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
        raise NotImplementedError(
            'post_terraform_setup not implemented while ' +
            'subclass indicated support via requires_post_terraform_setup')

    def cleanup(self):
        print(" - TODO: cleanup. Upload log files.")

    def pre_terraform_destroy(self):
        pass

    def post_terraform_destroy(self):
        pass
