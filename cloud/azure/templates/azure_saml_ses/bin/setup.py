#! /usr/bin/env python3

import shutil
import subprocess
import tempfile

from cloud.shared.bin.lib.setup_template import SetupTemplate

"""
Template Setup

This script handles the setup for the specific template. Calls out
to many different shell script in order to setup the environment
outside of the terraform setup. The setup is in two phases: pre_terraform_setup
and post_terraform_setup.
"""
class Setup(SetupTemplate):
    resource_group = None
    key_vault_name = None
    log_file_path = None

    def requires_post_terraform_setup(self):
        return True

    def pre_terraform_setup(self):
        print(" - Setting up shared state")
        self._setup_shared_state()
        print(" - Setting up the keyvault")
        self._setup_keyvault()
        print(" - Setting up the SAML keystore")
        self._setup_saml_keystore()
        print(" - Setting up SES")
        self._setup_ses()
        # Only run in dev mode
        if not self.config.use_backend_config():
            self._make_backend_override()

    def setup_log_file(self):
        self._setup_resource_group()
        _, self.log_file_path = tempfile.mkstemp()
        subprocess.run([
            "cloud/azure/bin/init-azure-log", self.log_file_path
        ], check=True)

    def post_terraform_setup(self):
        self._get_adfs_user_inputs()
        self._configure_slot_settings()

    def cleanup(self):
        self._upload_log_file()
        subprocess.run(["/bin/bash", "-c", "rm -f $HOME/.ssh/bastion*"], check=True)

    def _configure_slot_settings(self):
        subprocess.run([
            "cloud/azure/bin/configure-slot-settings"
        ], check=True)

    def _upload_log_file(self):
        subprocess.run([
            "cloud/azure/bin/upload-log-file", self.log_file_path
        ], check=True)

    def _get_adfs_user_inputs(self):
        print(">>>> You will need to navigate to https://portal.azure.com/ and "
              + "select the app_service that was created. Select authentication"
              + ", and add a new Microsoft identity provider. Select 'Allow "
              + "unauthenticated access'. Get the App (client) id.")
        self._input_to_keystore("adfs-client-id")

        print(">>>> Navigate to the newly created authentication"
              + " provider and click the endpoints button from the overview and"
              + " find the OpenID uri (ends with /openid-configuration).")
        self._input_to_keystore("adfs-discovery-uri")

        print(">>>> In the same view (authentication provider page), navigate "
              + " to certificates & secrets page, and add a new client secret. "
              + "Copy the value.")
        self._input_to_keystore("adfs-secret")

    def _input_to_keystore(self, secret_id):
        key_vault_name = self.config.get_config_var("KEY_VAULT_NAME")
        subprocess.run([
            "cloud/azure/bin/input-secrets-to-keystore",
            "-k", key_vault_name,
            "-s", secret_id],
            check=True)

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

    def _make_backend_override(self):
        current_directory = self.config.get_template_dir()
        shutil.copy2(f'{current_directory}/backend_override', f'{current_directory}/backend_override.tf')

    def _setup_shared_state(self):
        if not self.resource_group:
            raise RuntimeError("Resource group required")
        if self.config.use_backend_config():
            subprocess.run([
                "cloud/azure/bin/setup_tf_shared_state",
                f"{self.config.get_template_dir()}/{self.config.backend_vars_filename}"
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
            raise RuntimeError("Key Vault Name Required")

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
