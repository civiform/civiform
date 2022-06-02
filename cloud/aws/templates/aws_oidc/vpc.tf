# Souce: https://learn.hashicorp.com/tutorials/terraform/aws-rds?in=terraform/aws

data "aws_availability_zones" "available" {}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "2.78.0"

  name                    = "civiform"
  cidr                    = "10.0.0.0/16"
  azs                     = data.aws_availability_zones.available.names
  private_subnets         = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  enable_dns_hostnames    = true
  enable_dns_support      = true
  map_public_ip_on_launch = false
}

resource "aws_db_subnet_group" "civiform" {
  name       = "civiform"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_security_group" "rds" {
  name   = "civiform_rds"
  vpc_id = module.vpc.vpc_id

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "civiform_rds"
  }
}

resource "aws_apprunner_vpc_connector" "connector" {
  vpc_connector_name = "civiform-connector"
  subnets            = module.vpc.private_subnets
  security_groups    = [aws_security_group.rds.id]
}