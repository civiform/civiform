variable "key_vault_name" {
  type        = string
  description = "Name of key vault where secrets are stored."
}

variable "key_vault_resource_group" {
  type = string
}

variable "saml_keystore_filename" {
  type        = string
  description = "The name of the keystore file to use for SAML auth"
}

variable "saml_keystore_resource_group" {
  type        = string
  description = "The resource group for the storage account where the keystore file is hosted"
}

variable "saml_keystore_account_name" {
  type        = string
  description = "The storage account where the SAML keystore file is hosted"
}

variable "saml_keystore_container_name" {
  type        = string
  description = "The name of the keystore file"
}
