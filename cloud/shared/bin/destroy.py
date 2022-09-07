"""
Destroy.py destroys Civiform deployment.
"""

from cloud.shared.bin.lib import terraform
from cloud.shared.bin.lib.config_loader import ConfigLoader
from cloud.shared.bin.lib.setup_class_loader import get_config_specific_destroy


def run(config: ConfigLoader):
    template_destroy = get_config_specific_destroy(config)
    template_destroy.pre_terraform_destroy()
    terraform.perform_apply(config, is_destroy=True)
    template_destroy.post_terraform_destroy()
