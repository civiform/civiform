terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "4.20.1"
    }
  }
}

provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Group       = "setup-${var.app_prefix}"
      Environment = "${var.civiform_mode}"
      Service     = "Civiform Setup"
    }
  }
}
