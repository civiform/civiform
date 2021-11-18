# Configure the Azure provider
terraform {
  required_providers {
    azurerm = {
      source  = "azurerm"
      version = "~> 2.65"
    }
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 2.13.0"
    }
  }

  required_version = ">= 0.14.9"
}

provider "azurerm" {
  features {}
}


# provider "docker" {
# }
#
# resource "docker_image" "civiform" {
#   name         = "civiform/civiform:latest"
#   keep_locally = false
# }

resource "azurerm_resource_group" "rg" {
  name     = "myTFResourceGroup"
  location = "eastus"
}

# resource "docker_container" "civiform" {
#   image = docker_image.civiform.latest
#   name  = "civiformfoo"
#   ports {
#     internal = 80
#     external = 8000
#   }
# }

resource "azurerm_container_group" "cg" {
  name                = "civiform-container-group"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  ip_address_type     = "public"
  dns_name_label      = "civiform-staging"
  os_type             = "Linux"

  container {
    name   = "dh-civiform"
    image  = "civiform/civiform:latest"
    cpu    = "0.5"
    memory = "1.5"

    environment_variables = {
      DB_JDBC_STRING = "jdbc:postgresql://${azurerm_postgresql_server.civiform.fqdn}:5432/postgres"
      DB_USERNAME    = azurerm_postgresql_server.civiform.administrator_login
      DB_PASSWORD    = azurerm_postgresql_server.civiform.administrator_login_password
      SECRET_KEY      = "insecure-secret-key"
    }

    ports {
      port     = 80
      protocol = "TCP"
    }
  }

  diagnostics {
    log_analytics {
      workspace_id = azurerm_log_analytics_workspace.civiform_logs.workspace_id
      workspace_key = azurerm_log_analytics_workspace.civiform_logs.primary_shared_key
    }
  }
}

resource "azurerm_log_analytics_workspace" "civiform_logs" {
  name                = "civiform-server-logs"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  sku                 = "PerGB2018"
  retention_in_days   = 30
}

resource "azurerm_postgresql_server" "civiform" {
  name                = "civiform-db"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name

  administrator_login          = "psqladmin"
  administrator_login_password = "H@Sh1CoR3!"

  // fqdn civiform-psqlserver.postgres.database.azure.com

  sku_name   = "GP_Gen5_4"
  version    = "11"
  storage_mb = 5120

  backup_retention_days        = 7
  geo_redundant_backup_enabled = false
  auto_grow_enabled            = true

  public_network_access_enabled    = false
  ssl_enforcement_enabled          = true
  ssl_minimal_tls_version_enforced = "TLS1_2"
}
