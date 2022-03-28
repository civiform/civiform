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
  backend "local" {}
  required_version = ">= 0.14.9"
}
