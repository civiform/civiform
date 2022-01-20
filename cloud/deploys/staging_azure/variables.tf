variable "sender_email_address" {
  type        = string
  description = "Email address that emails will be sent from"
}

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

variable "postgres_admin_password" {
  type        = string
  description = "Postgres admin password"
}

variable "app_secret_key" {
  type        = string
  description = "Secret Key For the app"
}
