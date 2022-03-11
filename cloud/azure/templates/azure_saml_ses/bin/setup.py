import subprocess
class Setup:
    def __init__(self, config):
        self.config=config
        self.backend_config_filename = "staging_azure_backend_vars"
    
    def run(self):         
        self.create_ssh_keyfile()
        self.setup_resource_group()
        self.setup_shared_state()
        self.setup_keyvault()
        self.setup_saml_keystore()
        self.setup_ses()    
    
    def setup_resource_group(self): 
        resource_group = self.config.get_config_var("AZURE_RESOURCE_GROUP")
        resource_group_location = self.config.get_config_var("AZURE_LOCATION")
        subprocess.run([
            "cloud/azure/bin/create_resource_group", 
            "-g",resource_group, 
            "-l", resource_group_location], 
                       check=True)
        self.resource_group = resource_group
        self.resource_group_location = resource_group_location

    def create_ssh_keyfile(self): 
        subprocess.run("ssh-keygen -q -t rsa -b 4096 -N '' -f $HOME/.ssh/bastion <<< y", check=True, shell=True)
    
    def setup_shared_state(self):
        if not self.resource_group: 
            raise RuntimeError("Resource group required")
        subprocess.run([
            "cloud/azure/bin/setup_tf_shared_state", 
            self.backend_config_filename], check=True)
    
    def setup_keyvault(self): 
        if not self.resource_group: 
            raise RuntimeError("Resource group required")
        key_vault_name = self.config.get_config_var("AZURE_RESOURCE_GROUP")
        subprocess.run([
            "cloud/azure/bin/setup-keyvault", 
            "-g", self.resource_group, 
            "-v", key_vault_name, "-l", self.resource_group_location], 
                       check=True)
        self.key_vault_name = key_vault_name
    
    def setup_saml_keystore(self): 
        if not self.resource_group: 
            raise RuntimeError("Resource group required")
        if not self.key_vault_name: 
            raise RuntimeError("Key Vault Setup Required")
        
        saml_keystore_storage_account = self.config.get_config_var("SAML_KEYSTORE_STORAGE_ACCOUNT")
        subprocess.run([
            "cloud/azure/bin/setup-saml-keystore",
            "-g", self.resource_group, 
            "-v", self.key_vault_name, 
            "-l", self.resource_group_location, 
            "-s", saml_keystore_storage_account],
                       check=True)
    
    def setup_ses(self): 
        if not self.key_vault_name: 
            raise RuntimeError("Key Vault Setup Required")
        aws_username = self.config.get_config_var("AWS_USERNAME")
        subprocess.run([
            "cloud/azure/bin/ses-to-keyvault", 
            "-v", self.key_vault_name,
            "-u", aws_username
        ], check=True)

    def cleanup(self): 
        # delete the keygen file? 
        # delete the backend config
        pass
    
    def get_backend_config_filename(self): 
        return self.backend_config_filename
    