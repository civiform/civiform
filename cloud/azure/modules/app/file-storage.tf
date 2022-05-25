resource "azurerm_subnet" "storage_subnet" {
  name                                           = "storage-subnet"
  resource_group_name                            = data.azurerm_resource_group.rg.name
  virtual_network_name                           = azurerm_virtual_network.civiform_vnet.name
  address_prefixes                               = ["10.0.8.0/24"]
  service_endpoints                              = ["Microsoft.Storage"]
  enforce_private_link_endpoint_network_policies = true
}

resource "azurerm_storage_account" "files_storage_account" {
  name                = "${var.application_name}${random_string.resource_code.result}"
  location            = data.azurerm_resource_group.rg.location
  resource_group_name = data.azurerm_resource_group.rg.name

  account_tier = "Standard"
  # https://docs.microsoft.com/en-us/azure/storage/common/storage-redundancy
  account_replication_type = "LRS"

  allow_nested_items_to_be_public = false
}

resource "azurerm_storage_container" "files_container" {
  name                  = "files"
  storage_account_name  = azurerm_storage_account.files_storage_account.name
  container_access_type = "private"
}

resource "azurerm_data_protection_backup_policy_blob_storage" "blob_storage_backup_policy" {
  name               = "storage-backup-policy"
  vault_id           = azurerm_data_protection_backup_vault.backup_vault.id
  retention_duration = "P30D"
}

resource "azurerm_data_protection_backup_instance_blob_storage" "blob_storage_backup_instance" {
  name               = "storage-backup-instance"
  vault_id           = azurerm_data_protection_backup_vault.backup_vault.id
  location           = data.azurerm_resource_group.rg.location
  storage_account_id = azurerm_storage_account.files_storage_account.id
  backup_policy_id   = azurerm_data_protection_backup_policy_blob_storage.blob_storage_backup_policy.id

  depends_on = [azurerm_role_assignment.storage_backup_contributor]
}
