terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "4.17.1"
    }
  }
  backend "s3" {
    bucket         = "civiform-aws-staging-log-bucket"
    key            = "tfstate/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "civiform-backend-lock-table"
    encrypt        = true
  }
}

provider "aws" {
  region = var.aws_region
}
