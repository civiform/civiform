import subprocess

"""
Template Setup

This script handles the setup for the specific template. Calls out 
to many different shell script in order to setup the environment
outside of the terraform setup. The setup is in two phases: pre_terraform_setup
and post_terraform_setup. 
"""
class Setup:
    resource_group = None
    key_vault_name = None
    
    def __init__(self, config, backend_config_filename):
        self.config=config
        self.backend_config_filename = backend_config_filename
    
    def requires_post_terraform_setup(self):
        return True
    
    def pre_terraform_setup(self):         
        self._create_ssh_keyfile()
        self._setup_resource_group()
        self._setup_shared_state()
        self._setup_keyvault()
        self._setup_saml_keystore()
        self._setup_ses()    
    
    def post_terraform_setup(self):
        # TODO ask the user for their adfs information here
        pass

    def cleanup(self): 
        subprocess.run("rm $HOME/.ssh/bastion", check=True, shell=True)
    
    def _setup_resource_group(self): 
        resource_group = self.config.get_config_var("AZURE_RESOURCE_GROUP")
        resource_group_location = self.config.get_config_var("AZURE_LOCATION")
        subprocess.run([
            "cloud/azure/bin/create_resource_group", 
            "-g",resource_group, 
            "-l", resource_group_location], 
                       check=True)
        self.resource_group = resource_group
        self.resource_group_location = resource_group_location

    def _create_ssh_keyfile(self): 
        subprocess.run("ssh-keygen -q -t rsa -b 4096 -N '' -f $HOME/.ssh/bastion <<< y", check=True, shell=True)
    
    def _setup_shared_state(self):
        if not self.resource_group: 
            raise RuntimeError("Resource group required")
        if self.config.use_backend_config(): 
            subprocess.run([
                "cloud/azure/bin/setup_tf_shared_state", 
                f"{self.config.get_template_dir()}/{self.backend_config_filename}"
            ], check=True)
    
    def _setup_keyvault(self): 
        if not self.resource_group: 
            raise RuntimeError("Resource group required")
        key_vault_name = self.config.get_config_var("KEY_VAULT_NAME")
        subprocess.run([
            "cloud/azure/bin/setup-keyvault", 
            "-g", self.resource_group, 
            "-v", key_vault_name, "-l", self.resource_group_location
        ], check=True)
        self.key_vault_name = key_vault_name
    
    def _setup_saml_keystore(self): 
        if not self.resource_group: 
            raise RuntimeError("Resource group required")
        if not self.key_vault_name: 
            raise RuntimeError("Key Vault Setup Required")
        
        saml_keystore_storage_account = self.config.get_config_var("SAML_KEYSTORE_ACCOUNT_NAME")
        subprocess.run([
            "cloud/azure/bin/setup-saml-keystore",
            "-g", self.resource_group, 
            "-v", self.key_vault_name, 
            "-l", self.resource_group_location, 
            "-s", saml_keystore_storage_account
        ], check=True)
    
    def _setup_ses(self): 
        if not self.key_vault_name: 
            raise RuntimeError("Key Vault Setup Required")
        aws_username = self.config.get_config_var("AWS_USERNAME")
        subprocess.run([
            "cloud/azure/bin/ses-to-keyvault", 
            "-v", self.key_vault_name,
            "-u", aws_username
        ], check=True)
