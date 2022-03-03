output "storage_access_key" {
  value     = data.azurerm_storage_account.saml_keystore_account.primary_access_key
  sensitive = true
}

output "keystore_password" {
  value     = data.azurerm_key_vault_secret.saml_keystore_pass.value
  sensitive = true
}

output "storage_account_name" {
  value = var.saml_keystore_account_name
}

output "storage_container_name" {
  value = var.saml_keystore_container_name
}

output "filename" {
  value = var.saml_keystore_filename
}
