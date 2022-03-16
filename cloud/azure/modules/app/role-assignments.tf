# Primary managed identity role assignments:

resource "azurerm_role_assignment" "storage_blob_delegator" {
  scope                = azurerm_storage_account.files_storage_account.id
  role_definition_name = "Storage Blob Delegator"
  principal_id         = azurerm_app_service.civiform_app.identity.0.principal_id
}

resource "azurerm_role_assignment" "key_vault_secrets_user" {
  scope                = data.azurerm_key_vault.civiform_key_vault.id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_app_service.civiform_app.identity.0.principal_id
}

resource "azurerm_role_assignment" "storage_blob_data_contributor" {
  scope                = azurerm_storage_account.files_storage_account.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_app_service.civiform_app.identity.0.principal_id
}

# Grant the app the role of storage account contributor, as the app needs 
# to set its own CORS rules
resource "azurerm_role_assignment" "storage_account_contributor" {
  scope                = azurerm_storage_account.files_storage_account.id
  role_definition_name = "Storage Account Contributor"
  principal_id         = azurerm_app_service.civiform_app.identity.0.principal_id
}

# Canary managed identity role assignments:

resource "azurerm_role_assignment" "storage_blob_delegator_canary" {
  scope                = azurerm_storage_account.files_storage_account.id
  role_definition_name = "Storage Blob Delegator"
  principal_id         = azurerm_app_service_slot.canary.identity.0.principal_id
}

resource "azurerm_role_assignment" "key_vault_secrets_user_canary" {
  scope                = data.azurerm_key_vault.civiform_key_vault.id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_app_service_slot.canary.identity.0.principal_id
}

resource "azurerm_role_assignment" "storage_blob_data_contributor_canary" {
  scope                = azurerm_storage_account.files_storage_account.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_app_service_slot.canary.identity.0.principal_id
}

resource "azurerm_role_assignment" "storage_account_contributor_canary" {
  scope                = azurerm_storage_account.files_storage_account.id
  role_definition_name = "Storage Account Contributor"
  principal_id         = azurerm_app_service_slot.canary.identity.0.principal_id
}
