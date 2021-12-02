# Configure the Azure provider
terraform {
  required_providers {
    azurerm = {
      source  = "azurerm"
      version = ">=2.65"
    }
  }

  required_version = ">= 0.14.9"
}

provider "azurerm" {
  features {}
}

resource "azurerm_resource_group" "rg" {
  name     = "myTFResourceGroup"
  location = "eastus"
}

resource "azurerm_virtual_network" "civiform_vnet" {
  name                = "civiform-vnet"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  address_space       = ["10.0.0.0/16"]
}

resource "azurerm_subnet" "server_subnet" {
  name                 = "server-subnet"
  resource_group_name  = azurerm_resource_group.rg.name
  virtual_network_name = azurerm_virtual_network.civiform_vnet.name
  address_prefixes     = ["10.0.2.0/24"]

  delegation {
    name = "app-service-delegation"

    service_delegation {
      name    = "Microsoft.Web/serverFarms"
      actions = ["Microsoft.Network/virtualNetworks/subnets/action"]
    }
  }
}

resource "azurerm_app_service_plan" "plan" {
  name                = "${azurerm_resource_group.rg.name}-plan"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name

  # Define Linux as Host OS
  kind     = "Linux"
  reserved = true # Mandatory for Linux plans

  # Choose size
  sku {
    tier     = "Standard"
    size     = "S1"
    capacity = "2"
  }
}

resource "azurerm_app_service" "civiform_app" {
  name                = var.application_name
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  app_service_plan_id = azurerm_app_service_plan.plan.id
  app_settings = {
    WEBSITES_ENABLE_APP_SERVICE_STORAGE = false
    PORT                                = 9000

    DOCKER_REGISTRY_SERVER_URL = "https://index.docker.io"

    DB_USERNAME    = "${azurerm_postgresql_server.civiform.administrator_login}@${azurerm_postgresql_server.civiform.name}"
    DB_PASSWORD    = azurerm_postgresql_server.civiform.administrator_login_password
    DB_JDBC_STRING = "jdbc:postgresql://${local.postgres_private_link}:5432/postgres?ssl=true&sslmode=require"

    SECRET_KEY = "insecure-secret-key"
  }
  # Configure Docker Image to load on start
  site_config {
    linux_fx_version                     = "DOCKER|${var.docker_username}/${var.docker_repository_name}:${var.image_tag_name}"
    always_on                            = true
    acr_use_managed_identity_credentials = true
    vnet_route_all_enabled               = true
  }

  identity {
    type = "SystemAssigned"
  }

}

resource "azurerm_app_service_virtual_network_swift_connection" "appservice_vnet_connection" {
  app_service_id = azurerm_app_service.civiform_app.id
  subnet_id      = azurerm_subnet.server_subnet.id
}

resource "azurerm_log_analytics_workspace" "civiform_logs" {
  name                = "civiform-server-logs"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  sku                 = "PerGB2018"
  retention_in_days   = 30
}

resource "azurerm_monitor_diagnostic_setting" "app_service_log_analytics" {
  name                       = "${var.application_name}_log_analytics"
  target_resource_id         = azurerm_app_service.civiform_app.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.civiform_logs.id

  log {
    category = "AppServiceAppLogs"

    retention_policy {
      enabled = false
    }
  }

  log {
    category = "AppServiceConsoleLogs"

    retention_policy {
      enabled = false
    }
  }

  log {
    category = "AppServiceHTTPLogs"

    retention_policy {
      enabled = false
    }
  }

  log {
    category = "AppServiceAuditLogs"

    retention_policy {
      enabled = false
    }
  }
  metric {
    category = "AllMetrics"

    retention_policy {
      enabled = false
    }
  }
}

resource "azurerm_postgresql_server" "civiform" {
  name                = "civiform-db"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name

  administrator_login          = "psqladmin"
  administrator_login_password = "H@Sh1CoR3!"

  // fqdn civiform-db.postgres.database.azure.com

  sku_name   = "GP_Gen5_4"
  version    = "11"
  storage_mb = 5120

  backup_retention_days        = 7
  geo_redundant_backup_enabled = false
  auto_grow_enabled            = true

  public_network_access_enabled = false

  ssl_enforcement_enabled          = true
  ssl_minimal_tls_version_enforced = "TLS1_2"
}

resource "azurerm_postgresql_database" "civiform" {
  name                = "civiform"
  resource_group_name = azurerm_resource_group.rg.name
  server_name         = azurerm_postgresql_server.civiform.name
  charset             = "utf8"
  collation           = "English_United States.1252"
}

# Configure private link
resource "azurerm_subnet" "postgres_subnet" {
  name                 = "postgres_subnet"
  resource_group_name  = azurerm_resource_group.rg.name
  virtual_network_name = azurerm_virtual_network.civiform_vnet.name
  address_prefixes     = ["10.0.4.0/24"]

  enforce_private_link_endpoint_network_policies = true
}

resource "azurerm_private_dns_zone" "privatelink" {
  name                = "privatelink.postgres.database.azure.com"
  resource_group_name = azurerm_resource_group.rg.name
}

resource "azurerm_private_dns_zone_virtual_network_link" "vnet_link" {
  name                  = "vnet-link-private-dns"
  resource_group_name   = azurerm_resource_group.rg.name
  private_dns_zone_name = azurerm_private_dns_zone.privatelink.name
  virtual_network_id    = azurerm_virtual_network.civiform_vnet.id
}

resource "azurerm_private_endpoint" "endpoint" {
  name                = "${azurerm_postgresql_server.civiform.name}-endpoint"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
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
