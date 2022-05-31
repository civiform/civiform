data "azurerm_key_vault" "civiform_key_vault" {
  name                = var.key_vault_name
  resource_group_name = data.azurerm_resource_group.rg.name
}

data "azurerm_key_vault_secret" "postgres_password" {
  name         = local.postgres_password_keyvault_id
  key_vault_id = data.azurerm_key_vault.civiform_key_vault.id
}

data "azurerm_key_vault_secret" "aws_secret_access_token" {
  name         = local.aws_secret_access_token
  key_vault_id = data.azurerm_key_vault.civiform_key_vault.id
}

data "azurerm_key_vault_secret" "aws_access_key_id" {
  name         = local.aws_access_key_id
  key_vault_id = data.azurerm_key_vault.civiform_key_vault.id
}

data "azurerm_key_vault_secret" "app_secret_key" {
  name         = local.app_secret_key_keyvault_id
  key_vault_id = data.azurerm_key_vault.civiform_key_vault.id
}

data "azurerm_key_vault_secret" "adfs_secret" {
  name         = local.adfs_secret_keyvault_id
  key_vault_id = data.azurerm_key_vault.civiform_key_vault.id
}
