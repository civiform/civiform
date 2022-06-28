#! /usr/bin/env python3

import os
import subprocess

from cloud.shared.bin.lib.setup_template import SetupTemplate
"""
Destroy the setup
"""


class Destroy(SetupTemplate):

    def pre_terraform_destroy(self):
        if not self.config.use_backend_config():
            self._make_backend_override()

    def post_terraform_destroy(self):
        # need to do the setup destroy here should be pretty simple tho
