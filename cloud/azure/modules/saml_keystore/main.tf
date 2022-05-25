data "azurerm_key_vault" "civiform_key_vault" {
  name                = var.key_vault_name
  resource_group_name = var.resource_group_name
}

data "azurerm_key_vault_secret" "saml_keystore_pass" {
  name         = local.saml_keystore_pass_id
  key_vault_id = data.azurerm_key_vault.civiform_key_vault.id
}

data "azurerm_storage_account" "saml_keystore_account" {
  name                = var.saml_keystore_account_name
  resource_group_name = var.resource_group_name
}

data "azurerm_storage_container" "saml_keystore_container" {
  name                 = var.saml_keystore_container_name
  storage_account_name = data.azurerm_storage_account.saml_keystore_account.name
}
