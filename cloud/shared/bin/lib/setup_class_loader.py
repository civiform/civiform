import importlib.util
import os

"""
load_class takes in a template dir and uses that to load the 
setup module which knows how to setup the template
"""
def load_class(template_dir): 
    spec = importlib.util.spec_from_file_location(
        "setup", f"{os.getcwd()}/{template_dir}/bin/setup.py")
    loaded_module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(loaded_module)
    return loaded_module.Setup
