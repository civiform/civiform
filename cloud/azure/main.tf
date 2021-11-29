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

resource "azurerm_subnet" "application_gateway_subnet" {
  name                 = "GatewaySubnet"
  resource_group_name  = azurerm_resource_group.rg.name
  virtual_network_name = azurerm_virtual_network.civiform_vnet.name
  address_prefixes     = ["10.0.1.0/24"]
}

resource "azurerm_subnet" "server_subnet" {
  name                 = "server-subnet"
  resource_group_name  = azurerm_resource_group.rg.name
  virtual_network_name = azurerm_virtual_network.civiform_vnet.name
  address_prefixes     = ["10.0.2.0/24"]
  service_endpoints    = ["Microsoft.Sql"]

  delegation {
    name = "acidelegationservice"

    service_delegation {
      name    = "Microsoft.ContainerInstance/containerGroups"
      actions = ["Microsoft.Network/virtualNetworks/subnets/join/action", "Microsoft.Network/virtualNetworks/subnets/prepareNetworkPolicies/action"]
    }
  }
}

resource "azurerm_postgresql_virtual_network_rule" "civiform" {
  name                                 = "sqlvnetrule"
  resource_group_name                  = azurerm_resource_group.rg.name
  server_name                          = azurerm_postgresql_server.civiform.name
  subnet_id                            = azurerm_subnet.server_subnet.id
  ignore_missing_vnet_service_endpoint = true
}

resource "azurerm_public_ip" "server_container_group_ip" {
  name                = "server-container-group-ip"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  allocation_method   = "Dynamic"
}

locals {
  backend_address_pool_name      = "${azurerm_virtual_network.civiform_vnet.name}-beap"
  frontend_port_name             = "${azurerm_virtual_network.civiform_vnet.name}-feport"
  frontend_ip_configuration_name = "${azurerm_virtual_network.civiform_vnet.name}-feip"
  http_setting_name              = "${azurerm_virtual_network.civiform_vnet.name}-be-htst"
  listener_name                  = "${azurerm_virtual_network.civiform_vnet.name}-httplstn"
  request_routing_rule_name      = "${azurerm_virtual_network.civiform_vnet.name}-rqrt"
  redirect_configuration_name    = "${azurerm_virtual_network.civiform_vnet.name}-rdrcfg"
}

# resource "azurerm_application_gateway" "network" {
#   name                = "civiform-appgateway"
#   resource_group_name = azurerm_resource_group.rg.name
#   location            = azurerm_resource_group.rg.location

#   sku {
#     name     = "Standard_Small"
#     tier     = "Standard"
#     capacity = 2
#   }

#   gateway_ip_configuration {
#     name      = "civiform-gateway-ip-configuration"
#     subnet_id = azurerm_subnet.server_subnet.id
#   }

#   frontend_port {
#     name = local.frontend_port_name
#     port = 80
#   }

#   frontend_ip_configuration {
#     name                 = local.frontend_ip_configuration_name
#     public_ip_address_id = azurerm_public_ip.server_container_group_ip.id
#   }

#   backend_address_pool {
#     name = local.backend_address_pool_name
#   }

#   backend_http_settings {
#     name                  = local.http_setting_name
#     cookie_based_affinity = "Disabled"
#     path                  = "/path1/"
#     port                  = 80
#     protocol              = "Http"
#     request_timeout       = 60
#   }

#   http_listener {
#     name                           = local.listener_name
#     frontend_ip_configuration_name = local.frontend_ip_configuration_name
#     frontend_port_name             = local.frontend_port_name
#     protocol                       = "Http"
#   }

#   request_routing_rule {
#     name                       = local.request_routing_rule_name
#     rule_type                  = "Basic"
#     http_listener_name         = local.listener_name
#     backend_address_pool_name  = local.backend_address_pool_name
#     backend_http_settings_name = local.http_setting_name
#   }
# }

resource "azurerm_network_profile" "civiform_network_profile" {
  name                = "civiform-network-profile"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name

  container_network_interface {
    name = "civiform-server-network-interface"

    ip_configuration {
      name      = "civiform-network-interface-ip-configuration"
      subnet_id = azurerm_subnet.server_subnet.id
    }
  }
}

resource "azurerm_container_group" "cg" {
  name                = "civiform-container-group"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  network_profile_id  = azurerm_network_profile.civiform_network_profile.id
  ip_address_type     = "Private"
  os_type             = "Linux"

  container {
    name   = "dh-civiform"
    image  = "civiform/civiform:latest"
    cpu    = "0.5"
    memory = "1.5"

    environment_variables = {
      # Azure specifically requires the username to be in password@hostname format. Note
      # the hostname in this case is not the fully qualified hostname.
      DB_USERNAME    = "${azurerm_postgresql_server.civiform.administrator_login}@${azurerm_postgresql_server.civiform.name}"
      DB_PASSWORD    = azurerm_postgresql_server.civiform.administrator_login_password
      DB_JDBC_STRING = "jdbc:postgresql://${azurerm_postgresql_server.civiform.fqdn}:5432/postgres?ssl=true&sslmode=require"

      SECRET_KEY = "insecure-secret-key"
    }

    # ports exposed on the individual containers
    ports {
      port     = 80
      protocol = "TCP"
    }
    ports {
      port     = 443
      protocol = "TCP"
    }
  }

  # ports exposed on the container group
  exposed_port {
    port     = 80
    protocol = "TCP"
  }
  exposed_port {
    port     = 443
    protocol = "TCP"
  }

  diagnostics {
    log_analytics {
      workspace_id  = azurerm_log_analytics_workspace.civiform_logs.workspace_id
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

  # TODO: configure a subnet and restrict access only to the application servers.
  public_network_access_enabled = true

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

resource "azurerm_postgresql_firewall_rule" "civiform" {
  name                = "civiform-db-firewall"
  resource_group_name = azurerm_resource_group.rg.name
  server_name         = azurerm_postgresql_server.civiform.name

  # TODO: replace with something more specific
  # All zeros here configures the filewall to accept all incoming connections
  # from anywhere inside Azure.
  start_ip_address = "0.0.0.0"
  end_ip_address   = "0.0.0.0"
}
