"""
Writes a tfvars file with the format
name="value"\n 

If we want to store non string values here we will need to add in the variables
and do a lil more advanced file writing
"""


class TfVarWriter:

    def __init__(self, filepath):
        self.filepath = filepath

    # a json of key: vals to turn into a tfvars
    def write_variables(self, config_vars: dict):
        with open(self.filepath, "w") as tf_vars_file:
            for name, definition in config_vars.items():
                if definition is not None:
                    tf_vars_file.write(f'{name.lower()}="{definition}"\n')
