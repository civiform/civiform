terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "3.72.0"
    }
    azurerm = {
      source  = "azurerm"
      version = ">=2.99"
    }
    random = {}
  }
  backend "azurerm" {}
  required_version = ">= 0.14.9"
}

module "app" {
  source = "../../modules/app"

  resource_group_name = var.azure_resource_group

  postgres_admin_login = var.postgres_admin_login

  # note that we must use GP tier
  postgres_sku_name = "GP_Gen5_2"

  image_tag_name = var.docker_tag
  
  civiform_applicant_auth_protocol = var.civiform_applicant_auth_protocol
  key_vault_name                   = var.key_vault_name

  application_name = var.application_name

  ses_sender_email = var.sender_email_address

  staging_program_admin_notification_mailing_list = var.staging_program_admin_notification_mailing_list
  staging_ti_notification_mailing_list            = var.staging_ti_notification_mailing_list
  staging_applicant_notification_mailing_list     = var.staging_applicant_notification_mailing_list

  adfs_admin_group = var.adfs_admin_group

  login_radius_api_key       = var.login_radius_api_key
  login_radius_metadata_uri  = var.login_radius_metadata_uri
  login_radius_saml_app_name = var.login_radius_saml_app_name
  saml_keystore_filename     = module.saml_keystore.filename

  # These two values need to match for PKCS12 keys
  saml_keystore_password    = module.saml_keystore.keystore_password
  saml_private_key_password = module.saml_keystore.keystore_password

  saml_keystore_storage_access_key     = module.saml_keystore.storage_access_key
  saml_keystore_storage_account_name   = module.saml_keystore.storage_account_name
  saml_keystore_storage_container_name = module.saml_keystore.storage_container_name
}

module "custom_hostname" {
  source              = "../../modules/custom_hostname"
  custom_hostname     = var.custom_hostname
  app_service_name    = module.app.app_service_name
  resource_group_name = module.app.resource_group_name
}

module "saml_keystore" {
  source                       = "../../modules/saml_keystore"
  key_vault_name               = var.key_vault_name
  saml_keystore_filename       = var.saml_keystore_filename
  saml_keystore_container_name = var.saml_keystore_container_name
  saml_keystore_account_name   = var.saml_keystore_account_name
  resource_group_name          = var.azure_resource_group
}

module "email_service" {
  source               = "../../../aws/modules/ses"
  sender_email_address = var.sender_email_address
}

module "program_admin_email" {
  source               = "../../../aws/modules/ses"
  sender_email_address = var.staging_program_admin_notification_mailing_list
}

module "ti_admin_email" {
  source               = "../../../aws/modules/ses"
  sender_email_address = var.staging_ti_notification_mailing_list
}

module "applicant_email" {
  source               = "../../../aws/modules/ses"
  sender_email_address = var.staging_applicant_notification_mailing_list
}
