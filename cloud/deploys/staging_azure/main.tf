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

  key_vault_name           = var.key_vault_name
  key_vault_resource_group = var.key_vault_resource_group

  application_name = var.application_name

  ses_sender_email = var.sender_email_address

  staging_program_admin_notification_mailing_list = var.staging_program_admin_notification_mailing_list
  staging_ti_notification_mailing_list            = var.staging_ti_notification_mailing_list
  staging_applicant_notification_mailing_list     = var.staging_applicant_notification_mailing_list

  custom_hostname = var.custom_hostname

  adfs_client_id     = var.adfs_client_id
  adfs_discovery_uri = var.adfs_discovery_uri
  adfs_admin_group   = var.adfs_admin_group
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

module "program_admin_email" {
  source               = "../../aws/modules/ses"
  sender_email_address = var.staging_program_admin_notification_mailing_list
}

module "ti_admin_email" {
  source               = "../../aws/modules/ses"
  sender_email_address = var.staging_ti_notification_mailing_list
}

module "applicant_email" {
  source               = "../../aws/modules/ses"
  sender_email_address = var.staging_applicant_notification_mailing_list
}
