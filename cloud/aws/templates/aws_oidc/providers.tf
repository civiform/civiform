terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "4.21.0"
    }
  }
  backend "s3" {}
}

provider "aws" {
  region = var.aws_region
}
