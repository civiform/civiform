variable "postgres_admin_login" {
  type        = string
  description = "Postgres admin login"
}

variable "application_name" {
  type        = string
  description = "Azure Web App Name"
}


variable "aws_region" {
  type        = string
  description = "Region where the AWS servers will live"
  default     = "us-east-1"
}

variable "sender_email_address" {
  type        = string
  description = "Email address that emails will be sent from"
}

variable "resource_group_name" {
  type = string
}

variable "key_vault_name" {
  type        = string
  description = "Name of key vault where secrets are stored."
}

variable "adfs_admin_group" {
  type        = string
  description = "Active Directory Federation Service group name"
}

variable "staging_program_admin_notification_mailing_list" {
  type        = string
  description = "Admin notification mailing list for staging"
}

variable "staging_ti_notification_mailing_list" {
  type        = string
  description = "intermediary notification mailing list for staging"
}

variable "staging_applicant_notification_mailing_list" {
  type        = string
  description = "Applicant notification mailing list for staging"
}

variable "login_radius_api_key" {
  type        = string
  description = "Login Radius API Key"
  default     = null
}

variable "login_radius_metadata_uri" {
  type        = string
  description = "LoginRadius endpoint for fetching IdP metadata"
  default     = null
}

variable "login_radius_saml_app_name" {
  type        = string
  description = "The App Name for the LoginRadius SAML integration"
  default     = null
}

variable "saml_keystore_filename" {
  type        = string
  description = "The name of the keystore file to use for SAML auth"
  default     = "civiformSamlKeystore.jks"
}

variable "saml_keystore_account_name" {
  type        = string
  description = "The storage account where the SAML keystore file is hosted"
}


variable "saml_keystore_container_name" {
  type        = string
  description = "The name of the keystore file"
  default     = "saml-keystore"
}
