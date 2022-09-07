import subprocess
import importlib
import os

from cloud.shared.bin.lib.config_loader import ConfigLoader


def run(config: ConfigLoader):
    deploy_file_py = os.path.join(
        'cloud', config.get_cloud_provider(), 'bin', 'deploy.py')
    # TODO(#2741): remove the fork after Azure scripts are in python
    if os.path.exists(deploy_file_py):
        deploy_module = importlib.import_module(
            f'cloud.{config.get_cloud_provider()}.bin.deploy')
        deploy_module.run(config)
    else:
        subprocess.check_call(
            os.path.join('cloud', config.get_cloud_provider(), 'bin', 'deploy'))
