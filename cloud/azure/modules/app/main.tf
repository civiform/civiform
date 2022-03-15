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

  allow_blob_public_access = false
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
  app_settings = {
    WEBSITES_ENABLE_APP_SERVICE_STORAGE = false
    PORT                                = 9000

    DOCKER_REGISTRY_SERVER_URL = "https://index.docker.io"

    DB_USERNAME    = "${azurerm_postgresql_server.civiform.administrator_login}@${azurerm_postgresql_server.civiform.name}"
    DB_PASSWORD    = "@Microsoft.KeyVault(SecretUri=${data.azurerm_key_vault_secret.postgres_password.id})"
    DB_JDBC_STRING = "jdbc:postgresql://${local.postgres_private_link}:5432/postgres?ssl=true&sslmode=require"

    STORAGE_SERVICE_NAME = "azure-blob"
    # this allows for the dev instances to get setup
    STAGING_HOSTNAME = (var.staging_hostname != "" ? var.staging_hostname : local.generated_hostname)
    BASE_URL         = "https://${var.custom_hostname != "" ? var.custom_hostname : local.generated_hostname}"

    AZURE_STORAGE_ACCOUNT_NAME      = azurerm_storage_account.files_storage_account.name
    AZURE_STORAGE_ACCOUNT_CONTAINER = azurerm_storage_container.files_container.name

    AWS_SES_SENDER        = var.ses_sender_email
    AWS_ACCESS_KEY_ID     = "@Microsoft.KeyVault(SecretUri=${data.azurerm_key_vault_secret.aws_access_key_id.id})"
    AWS_SECRET_ACCESS_KEY = "@Microsoft.KeyVault(SecretUri=${data.azurerm_key_vault_secret.aws_secret_access_token.id})"
    AWS_REGION            = var.aws_region

    STAGING_ADMIN_LIST     = var.staging_program_admin_notification_mailing_list
    STAGING_TI_LIST        = var.staging_ti_notification_mailing_list
    STAGING_APPLICANT_LIST = var.staging_applicant_notification_mailing_list

    SECRET_KEY = "@Microsoft.KeyVault(SecretUri=${data.azurerm_key_vault_secret.app_secret_key.id})"

    ADFS_CLIENT_ID     = var.adfs_client_id
    ADFS_SECRET        = "@Microsoft.KeyVault(SecretUri=${data.azurerm_key_vault_secret.adfs_secret.id})"
    ADFS_DISCOVERY_URI = var.adfs_discovery_uri

    CIVIFORM_APPLICANT_IDP = var.civiform_applicant_idp

    # The values below are all defaulted to null. If SAML authentication is used, the values can be pulled from the
    # saml_keystore module
    LOGIN_RADIUS_METADATA_URI     = var.login_radius_metadata_uri
    LOGIN_RADIUS_API_KEY          = var.login_radius_api_key
    LOGIN_RADIUS_SAML_APP_NAME    = var.login_radius_saml_app_name
    LOGIN_RADIUS_KEYSTORE_NAME    = (var.saml_keystore_filename != null ? "/saml/${var.saml_keystore_filename}" : "")
    LOGIN_RADIUS_KEYSTORE_PASS    = var.saml_keystore_password
    LOGIN_RADIUS_PRIVATE_KEY_PASS = var.saml_private_key_password

    # In HOCON, env variables set to the empty string are 
    # kept as such (set to empty string, rather than undefined).
    # This allows for the default to include atallclaims and for 
    # azure AD to not include that claim.
    ADFS_ADDITIONAL_SCOPES = ""
  }
  # Configure Docker Image to load on start
  site_config {
    always_on                            = true
    vnet_route_all_enabled               = true
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
}

resource "azurerm_app_service_slot" "canary" {
  name                = "canary"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  app_service_plan_id = azurerm_app_service_plan.plan.id
  app_service_name    = azurerm_app_service.civiform_app.name
  
  app_settings = local.app_settings

  site_config {
    always_on                            = true
    vnet_route_all_enabled               = true
  }

  # We will only mount this storage container if SAML authentication is being used
  dynamic "storage_account" {
    for_each = var.civiform_applicant_auth_protocol == "saml" ? [1] : [0]
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
}

resource "azurerm_app_service_virtual_network_swift_connection" "appservice_vnet_connection" {
  app_service_id = azurerm_app_service.civiform_app.id
  subnet_id      = azurerm_subnet.server_subnet.id
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
