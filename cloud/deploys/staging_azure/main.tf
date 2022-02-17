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
  backend "azurerm" {}
  required_version = ">= 0.14.9"
}

module "app" {
  source               = "../../azure/modules/app"
  postgres_admin_login = var.postgres_admin_login

  # note that we must use GP tier
  postgres_sku_name = "GP_Gen5_2"

  docker_username        = var.docker_username
  docker_repository_name = var.docker_repository_name

  key_vault_name = var.key_vault_name
  key_vault_resource_group = var.key_vault_resource_group

  application_name = var.application_name

  ses_sender_email = var.sender_email_address
  custom_hostname  = var.custom_hostname
}

module "custom_hostname" {
  source              = "../../azure/modules/custom_hostname"
  custom_hostname     = var.custom_hostname
  app_service_name    = module.app.app_service_name
  resource_group_name = module.app.resource_group_name
}

module "email_service" {
  source               = "../../aws/modules/ses"
  sender_email_address = var.sender_email_address
}
