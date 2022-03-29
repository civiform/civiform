resource "random_pet" "server" {}

resource "random_string" "resource_code" {
  length  = 5
  special = false
  upper   = false
}

data "azurerm_resource_group" "rg" {
  name = var.resource_group_name
}

resource "azurerm_virtual_network" "civiform_vnet" {
  name                = "civiform-vnet"
  location            = data.azurerm_resource_group.rg.location
  resource_group_name = data.azurerm_resource_group.rg.name
  address_space       = var.vnet_address_space
}

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

  account_tier             = "Standard"
  account_replication_type = "LRS"

  allow_nested_items_to_be_public = false
}

data "azurerm_key_vault_secret" "adfs_client_id" {
  name         = local.adfs_client_id
  key_vault_id = data.azurerm_key_vault.civiform_key_vault.id
}

data "azurerm_key_vault_secret" "adfs_discovery_uri" {
  name         = local.adfs_discovery_uri
  key_vault_id = data.azurerm_key_vault.civiform_key_vault.id
}

resource "azurerm_storage_container" "files_container" {
  name                  = "files"
  storage_account_name  = azurerm_storage_account.files_storage_account.name
  container_access_type = "private"
}

resource "azurerm_subnet" "server_subnet" {
  name                 = "server-subnet"
  resource_group_name  = data.azurerm_resource_group.rg.name
  virtual_network_name = azurerm_virtual_network.civiform_vnet.name
  address_prefixes     = var.subnet_address_prefixes

  delegation {
    name = "app-service-delegation"

    service_delegation {
      name    = "Microsoft.Web/serverFarms"
      actions = ["Microsoft.Network/virtualNetworks/subnets/action"]
    }
  }
}

resource "azurerm_subnet" "canary_subnet" {
  name                 = "canary-subnet"
  resource_group_name  = data.azurerm_resource_group.rg.name
  virtual_network_name = azurerm_virtual_network.civiform_vnet.name
  address_prefixes     = var.canary_subnet_address_prefixes

  delegation {
    name = "app-service-delegation"

    service_delegation {
      name    = "Microsoft.Web/serverFarms"
      actions = ["Microsoft.Network/virtualNetworks/subnets/action"]
    }
  }
}

resource "azurerm_app_service_plan" "plan" {
  name                = "${data.azurerm_resource_group.rg.name}-plan"
  location            = data.azurerm_resource_group.rg.location
  resource_group_name = data.azurerm_resource_group.rg.name

  # Define Linux as Host OS
  kind     = "Linux"
  reserved = true # Mandatory for Linux plans

  # Choose size
  sku {
    tier     = var.app_sku["tier"]
    size     = var.app_sku["size"]
    capacity = var.app_sku["capacity"]
  }
}

resource "azurerm_app_service" "civiform_app" {
  name                = "${var.application_name}-${random_pet.server.id}"
  location            = data.azurerm_resource_group.rg.location
  resource_group_name = data.azurerm_resource_group.rg.name
  app_service_plan_id = azurerm_app_service_plan.plan.id

  app_settings = local.app_settings

  site_config {
    linux_fx_version       = "DOCKER|${var.docker_username}/${var.docker_repository_name}:${var.image_tag}"
    always_on              = true
    vnet_route_all_enabled = true
  }

  # We will only mount this storage container if SAML authentication is being used
  dynamic "storage_account" {
    for_each = var.civiform_applicant_auth_protocol == "saml" ? [1] : []

    content {
      name         = "civiform-saml-keystore"
      type         = "AzureBlob"
      account_name = var.saml_keystore_storage_account_name
      share_name   = var.saml_keystore_storage_container_name
      access_key   = var.saml_keystore_storage_access_key
      mount_path   = "/saml"
    }
  }

  identity {
    type = "SystemAssigned"
  }

  logs {
    http_logs {
      file_system {
        retention_in_days = 1
        retention_in_mb   = 35
      }
    }
  }

  lifecycle {
    ignore_changes = [
      app_settings["STAGING_HOSTNAME"],
      app_settings["BASE_URL"],
      site_config[0].linux_fx_version
    ]
  }
}

resource "azurerm_app_service_slot" "canary" {
  name                = "canary"
  location            = data.azurerm_resource_group.rg.location
  resource_group_name = data.azurerm_resource_group.rg.name
  app_service_plan_id = azurerm_app_service_plan.plan.id
  app_service_name    = azurerm_app_service.civiform_app.name

  app_settings = local.app_settings

  site_config {
    linux_fx_version       = "DOCKER|${var.docker_username}/${var.docker_repository_name}:${var.image_tag}"
    always_on              = true
    vnet_route_all_enabled = true
  }

  # We will only mount this storage container if SAML authentication is being used
  dynamic "storage_account" {
    for_each = var.civiform_applicant_auth_protocol == "saml" ? [1] : []
    content {
      name         = "civiform-saml-keystore"
      type         = "AzureBlob"
      account_name = var.saml_keystore_storage_account_name
      share_name   = var.saml_keystore_storage_container_name
      access_key   = var.saml_keystore_storage_access_key
      mount_path   = "/saml"
    }
  }

  identity {
    type = "SystemAssigned"
  }

  logs {
    http_logs {
      file_system {
        retention_in_days = 1
        retention_in_mb   = 35
      }
    }
  }

  lifecycle {
    ignore_changes = [
      app_settings["STAGING_HOSTNAME"],
      app_settings["BASE_URL"],
      site_config[0].linux_fx_version
    ]
  }
}

resource "azurerm_app_service_virtual_network_swift_connection" "appservice_vnet_connection" {
  app_service_id = azurerm_app_service.civiform_app.id
  subnet_id      = azurerm_subnet.server_subnet.id
}

resource "azurerm_app_service_slot_virtual_network_swift_connection" "canary_vnet_connection" {
  app_service_id = azurerm_app_service.civiform_app.id
  subnet_id      = azurerm_subnet.server_subnet.id
  slot_name      = azurerm_app_service_slot.canary.name
}

resource "azurerm_postgresql_server" "civiform" {
  name                = "civiform-${random_pet.server.id}"
  location            = data.azurerm_resource_group.rg.location
  resource_group_name = data.azurerm_resource_group.rg.name

  administrator_login          = var.postgres_admin_login
  administrator_login_password = data.azurerm_key_vault_secret.postgres_password.value

  // fqdn civiform-db.postgres.database.azure.com

  sku_name   = var.postgres_sku_name
  version    = "11"
  storage_mb = var.postgres_storage_mb

  backup_retention_days        = var.postgres_backup_retention_days
  geo_redundant_backup_enabled = false
  auto_grow_enabled            = true

  public_network_access_enabled = false

  ssl_enforcement_enabled          = true
  ssl_minimal_tls_version_enforced = "TLS1_2"
}

resource "azurerm_postgresql_database" "civiform" {
  name                = "civiform"
  resource_group_name = data.azurerm_resource_group.rg.name
  server_name         = azurerm_postgresql_server.civiform.name
  charset             = "utf8"
  collation           = "English_United States.1252"
}

# Configure private link
resource "azurerm_subnet" "postgres_subnet" {
  name                 = "postgres_subnet"
  resource_group_name  = data.azurerm_resource_group.rg.name
  virtual_network_name = azurerm_virtual_network.civiform_vnet.name
  address_prefixes     = var.postgres_subnet_address_prefixes

  enforce_private_link_endpoint_network_policies = true
}

resource "azurerm_private_dns_zone" "privatelink" {
  name                = "privatelink.postgres.database.azure.com"
  resource_group_name = data.azurerm_resource_group.rg.name
}

resource "azurerm_private_dns_zone_virtual_network_link" "vnet_link" {
  name                  = "vnet-link-private-dns"
  resource_group_name   = data.azurerm_resource_group.rg.name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink.name
  virtual_network_id    = azurerm_virtual_network.civiform_vnet.id
}

resource "azurerm_private_endpoint" "endpoint" {
  name                = "${azurerm_postgresql_server.civiform.name}-endpoint"
  location            = data.azurerm_resource_group.rg.location
  resource_group_name = data.azurerm_resource_group.rg.name
  subnet_id           = azurerm_subnet.postgres_subnet.id

  private_dns_zone_group {
    name                 = "private-dns-zone-group"
    private_dns_zone_ids = [azurerm_private_dns_zone.privatelink.id]
  }

  private_service_connection {
    name                           = "${azurerm_postgresql_server.civiform.name}-privateserviceconnection"
    private_connection_resource_id = azurerm_postgresql_server.civiform.id
    subresource_names              = ["postgresqlServer"]
    is_manual_connection           = false
  }
}
module "bastion" {
  source = "../bastion"

  resource_group_name      = data.azurerm_resource_group.rg.name
  resource_group_location  = data.azurerm_resource_group.rg.location
  bastion_address_prefixes = var.bastion_address_prefixes
  vnet_name                = azurerm_virtual_network.civiform_vnet.name
}
