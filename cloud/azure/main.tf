# Configure the Azure provider
terraform {
  required_providers {
    azurerm = {
      source  = "azurerm"
      version = ">=2.65"
    }
    random = {}
  }
  backend "azurerm" {}
  required_version = ">= 0.14.9"
}

provider "azurerm" {
  features {}
}
