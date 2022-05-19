resource "aws_apprunner_service" "civiform_dev_leyla" {                                               
         
  service_name = "civiform_dev_leyla"                          
                            
  source_configuration {                              
    image_repository {                                
      image_configuration {                                  
        port = "8000"  

        runtime_environment_variables = {
            SECRET_KEY = "secretkeyplaceholder"
            PORT = "9000"

            DB_JDBC_STRING = "jdbc:postgresql://${aws_db_instance.civiform.address}:${aws_db_instance.civiform.port}/postgres?ssl=true&sslmode=require"
            DB_USERNAME = aws_db_instance.civiform.username
            DB_PASSWORD = aws_db_instance.civiform.password

            STAGING_HOSTNAME = "staging-aws.civiform.dev"
            BASE_URL         = "staging-aws.civiform.dev"

            STORAGE_SERVICE_NAME = "s3"

            CIVIFORM_TIME_ZONE_ID              = var.civiform_time_zone_id
            WHITELABEL_CIVIC_ENTITY_SHORT_NAME = var.civic_entity_short_name
            WHITELABEL_CIVIC_ENTITY_FULL_NAME  = var.civic_entity_full_name
            WHITELABEL_SMALL_LOGO_URL          = var.civic_entity_small_logo_url
            WHITELABEL_LOGO_WITH_NAME_URL      = var.civic_entity_logo_with_name_url
            SUPPORT_EMAIL_ADDRESS              = var.civic_entity_support_email_address

            AWS_SES_SENDER        = var.ses_sender_email
            AWS_ACCESS_KEY_ID     = ""
            AWS_SECRET_ACCESS_KEY = ""
            AWS_REGION            = var.aws_region

            STAGING_ADMIN_LIST     = var.staging_program_admin_notification_mailing_list
            STAGING_TI_LIST        = var.staging_ti_notification_mailing_list
            STAGING_APPLICANT_LIST = var.staging_applicant_notification_mailing_list

            AD_GROUPS_ATTRIBUTE_NAME = var.ad_groups_attribute_name
            ADFS_SECRET              = ""
            ADFS_CLIENT_ID           = ""
            ADFS_DISCOVERY_URI       = ""
            ADFS_GLOBAL_ADMIN_GROUP  = var.adfs_admin_group
            CIVIFORM_APPLICANT_IDP   = var.civiform_applicant_idp

            ADFS_ADDITIONAL_SCOPES = ""
        }                              
      }                                
      
      image_identifier       = "public.ecr.aws/t1q6b4h2/universal-application-tool:prod"                                
      image_repository_type = "ECR_PUBLIC"                              
    }                          
                              
    auto_deployments_enabled = false                            
  }                          
}

data "aws_route53_zone" "civiform" {
  name = "staging-aws.civiform.dev."
}

resource "aws_apprunner_custom_domain_association" "civiform_domain" {
  domain_name = "staging-aws.civiform.dev"
  service_arn = aws_apprunner_service.civiform_dev_leyla.arn
}

resource "aws_route53_record" "civiform_domain_record" {
  name = "staging-aws.civiform.dev"
  zone_id = data.aws_route53_zone.civiform.zone_id
  type = "CNAME"
  records = [aws_apprunner_custom_domain_association.civiform_domain.dns_target]
  ttl = 60
}

resource "aws_route53_record" "civiform_domain_validation" {
  for_each = {
    for dvo in aws_apprunner_custom_domain_association.civiform_domain.certificate_validation_records : dvo.name => {
      name = dvo.name
      type = dvo.type
      record = dvo.value
    }
  }
  
  name = each.value.name
  zone_id = data.aws_route53_zone.civiform.zone_id
  type = each.value.type
  records = [each.value.record]
  ttl = 60
}

output "custom_domain_records" {                            
  value = aws_apprunner_custom_domain_association.civiform_domain.certificate_validation_records                      
}

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

resource "aws_db_parameter_group" "civiform" {
  name   = "civiform"
  family = "postgres11"

  parameter {
    name  = "log_connections"
    value = "1"
  }
}

resource "aws_db_instance" "civiform" {
  identifier             = "civiform"
  instance_class         = "db.t3.micro"
  allocated_storage      = 5
  engine                 = "postgres"
  engine_version         = "11"
  username               = "leyla"
  password               = "changeme"
  db_subnet_group_name   = aws_db_subnet_group.civiform.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  parameter_group_name   = aws_db_parameter_group.civiform.name
  publicly_accessible    = true
  skip_final_snapshot    = true
}