variable "postgres_admin_login" {
  type        = string
  description = "Postgres admin login"
}

variable "docker_username" {
  type        = string
  description = "Docker username"
}

variable "docker_repository_name" {
  type        = string
  description = "Name of container image"
}

variable "application_name" {
  type        = string
  description = "Azure Web App Name"
}

variable "key_vault_name" {
  type        = string
  description = "Name of key vault where secrets are stored."
}

variable "key_vault_resource_group" {
  type        = string
  description = "Resource group that key vault is in."
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

variable "custom_hostname" {
  type        = string
  description = "custom hostname for the app to map the dns (used also for CORS)"
  default     = "staging-azure.civiform.dev"
}

variable "adfs_client_id" {
  type        = string
  description = "Active Directory Federation Service client id"
}

variable "adfs_discovery_uri" {
  type        = string
  description = "Active Directory Federation Service url that handles adfs login"
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

variable "civiform_applicant_auth_protocol" {
  type        = string
  description = "auth protocol to use for applicant auth. supported values are oidc and saml"
}
