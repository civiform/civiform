provider "azurerm" {
  features {}
}

provider "aws" {
  region = var.aws_region
}
