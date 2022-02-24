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

variable "app_secret_key" {
  type        = string
  description = "Secret Key For the app"
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

variable "key_vault_resource_group" {
  type = string
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
