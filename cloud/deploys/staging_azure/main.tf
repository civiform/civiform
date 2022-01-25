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
  source = "../../azure/modules/app"

  postgres_admin_login    = var.postgres_admin_login
  postgres_admin_password = var.postgres_admin_password

  docker_username        = var.docker_username
  docker_repository_name = var.docker_repository_name

  application_name = var.application_name
  app_secret_key   = var.app_secret_key
  ses_sender_email = var.sender_email_address

  staging_hostname = var.staging_hostname
}

module "email_service" {
  source               = "../../aws/modules/ses"
  sender_email_address = var.sender_email_address
}
