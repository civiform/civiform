"""
load_class takes in a template dir and uses that to load the
setup module which knows how to setup the template
"""

import importlib.util
import os

from cloud.shared.bin.lib.config_loader import ConfigLoader
from cloud.shared.bin.lib.setup_template import SetupTemplate


def get_config_specific_setup(config: ConfigLoader) -> SetupTemplate:
    spec = importlib.util.spec_from_file_location(
        "setup", f"{os.getcwd()}/{config.get_template_dir()}/bin/setup.py")
    loaded_module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(loaded_module)
    return loaded_module.Setup(config)


def get_config_specific_destroy(config: ConfigLoader) -> SetupTemplate:
    spec = importlib.util.spec_from_file_location(
        "destroy", f"{os.getcwd()}/{config.get_template_dir()}/bin/destroy.py")
    loaded_module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(loaded_module)
    return loaded_module.Destroy(config)
