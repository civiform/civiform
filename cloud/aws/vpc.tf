# Souce: https://learn.hashicorp.com/tutorials/terraform/aws-rds?in=terraform/aws

# Do this: https://www.stackovercloud.com/2022/02/09/new-for-app-runner-vpc-support/ 

data "aws_availability_zones" "available" {}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "2.77.0"

  name                 = "civiform"
  cidr                 = "10.0.0.0/16"
  azs                  = data.aws_availability_zones.available.names
  public_subnets       = ["10.0.4.0/24", "10.0.5.0/24", "10.0.6.0/24"]
  enable_dns_hostnames = true
  enable_dns_support   = true
}

resource "aws_db_subnet_group" "civiform" {
  name       = "civiform"
  subnet_ids = module.vpc.public_subnets

  tags = {
    Name = "civiform"
  }
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

# https://github.com/aws-containers/impression-counter-api-app-runner-sample/blob/main/terraform/apprunner.tf
# resource "aws_apprunner_vpc_connector" "db-connector" {
#   vpc_connector_name = "civiform-app-connector"
#   subnets            = ["${aws_db_subnet_group.civiform}"]
#   security_groups    = ["${aws_security_group.rds}"]
# }

# resource "aws_vpc_endpoint" "s3" {
#   vpc_id       = module.vpc.vpc_id
#   service_name = "com.amazonaws.${var.aws_region}.s3"
# }