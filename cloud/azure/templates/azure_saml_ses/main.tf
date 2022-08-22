terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "4.27.0"
    }
    azurerm = {
      source  = "azurerm"
      version = "3.19.1"
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

  image_tag = var.image_tag

  civiform_applicant_auth_protocol = var.civiform_applicant_auth_protocol
  key_vault_name                   = var.key_vault_name

  application_name = var.application_name

  ses_sender_email = var.sender_email_address

  staging_program_admin_notification_mailing_list = var.staging_program_admin_notification_mailing_list
  staging_ti_notification_mailing_list            = var.staging_ti_notification_mailing_list
  staging_applicant_notification_mailing_list     = var.staging_applicant_notification_mailing_list

  civiform_time_zone_id = var.civiform_time_zone_id

  civic_entity_short_name            = var.civic_entity_short_name
  civic_entity_full_name             = var.civic_entity_full_name
  civic_entity_support_email_address = var.civic_entity_support_email_address
  civic_entity_logo_with_name_url    = var.civic_entity_logo_with_name_url
  civic_entity_small_logo_url        = var.civic_entity_small_logo_url

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

  feature_flag_status_tracking_enabled = var.feature_flag_status_tracking_enabled
  civiform_api_keys_ban_global_subnet  = var.civiform_api_keys_ban_global_subnet
}

module "custom_hostname" {
  for_each            = var.custom_hostname != "" ? toset([var.custom_hostname]) : toset([])
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
  for_each = toset([
    var.sender_email_address,
    var.staging_applicant_notification_mailing_list,
    var.staging_ti_notification_mailing_list,
    var.staging_program_admin_notification_mailing_list
  ])
  source               = "../../../aws/modules/ses"
  sender_email_address = each.key
}
