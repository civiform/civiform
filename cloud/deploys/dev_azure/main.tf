terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "3.72.0"
    }
    azurerm = {
      source  = "azurerm"
      version = ">=2.65"
    }
    random = {}
  }
  required_version = ">= 0.14.9"
}

module "app" {
  source               = "../../azure/modules/app"
  resource_group_name  = var.resource_group_name
  postgres_admin_login = var.postgres_admin_login
  postgres_sku_name    = "GP_Gen5_2"

  docker_username        = var.docker_username
  docker_repository_name = var.docker_repository_name

  key_vault_name           = var.key_vault_name
  key_vault_resource_group = var.key_vault_resource_group

  application_name = var.application_name
  ses_sender_email = var.sender_email_address
  custom_hostname  = ""
  staging_hostname = ""

  adfs_client_id     = var.adfs_client_id
  adfs_discovery_uri = var.adfs_discovery_uri
  adfs_admin_group   = var.adfs_admin_group
}
