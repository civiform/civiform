# TODO: split this into modules.
module "secrets" {
  source = "../../modules/secrets_manager"
}

resource "aws_apprunner_service" "civiform_dev" {
  auto_scaling_configuration_arn = aws_apprunner_auto_scaling_configuration_version.auto_scaling_config.arn
  service_name                   = "civiform_dev"

  source_configuration {
    image_repository {
      image_configuration {
        port = "9000"

        runtime_environment_variables = {
          SECRET_KEY = module.secrets.app_secret_key_version
          PORT       = "9000"

          DB_JDBC_STRING = "jdbc:postgresql://${aws_db_instance.civiform.address}:${aws_db_instance.civiform.port}/postgres?ssl=true&sslmode=require"
          DB_USERNAME    = aws_db_instance.civiform.username
          DB_PASSWORD    = aws_db_instance.civiform.password

          STAGING_HOSTNAME = "staging-aws.civiform.dev"
          BASE_URL         = "https://staging-aws.civiform.dev"

          STORAGE_SERVICE_NAME = "s3"
          AWS_S3_BUCKET_NAME   = "${aws_s3_bucket.civiform_files_s3.id}"

          CIVIFORM_TIME_ZONE_ID              = var.civiform_time_zone_id
          WHITELABEL_CIVIC_ENTITY_SHORT_NAME = var.civic_entity_short_name
          WHITELABEL_CIVIC_ENTITY_FULL_NAME  = var.civic_entity_full_name
          WHITELABEL_SMALL_LOGO_URL          = var.civic_entity_small_logo_url
          WHITELABEL_LOGO_WITH_NAME_URL      = var.civic_entity_logo_with_name_url
          SUPPORT_EMAIL_ADDRESS              = var.civic_entity_support_email_address

          AWS_SES_SENDER = var.ses_sender_email
          AWS_REGION     = var.aws_region

          STAGING_ADMIN_LIST     = var.staging_program_admin_notification_mailing_list
          STAGING_TI_LIST        = var.staging_ti_notification_mailing_list
          STAGING_APPLICANT_LIST = var.staging_applicant_notification_mailing_list
        }
      }

      image_identifier      = "${var.civiform_image_repo}:${var.civiform_image_tag}"
      image_repository_type = "ECR_PUBLIC"
    }

    auto_deployments_enabled = false
  }

  network_configuration {
    egress_configuration {
      egress_type       = "VPC"
      vpc_connector_arn = aws_apprunner_vpc_connector.connector.arn
    }
  }
  instance_configuration {
    instance_role_arn = aws_iam_role.apprunner_instance_role.arn
  }
}

resource "aws_db_parameter_group" "civiform" {
  name   = "civiform"
  family = "postgres12"

  parameter {
    name  = "log_connections"
    value = "1"
  }
}

resource "aws_db_instance" "civiform" {
  identifier              = var.postgress_name
  instance_class          = var.postgres_instance_class
  allocated_storage       = var.postgres_storage_gb
  engine                  = "postgres"
  engine_version          = "12"
  username                = "db_user_name"
  password                = module.secrets.database_password
  db_subnet_group_name    = aws_db_subnet_group.civiform.name
  vpc_security_group_ids  = [aws_security_group.rds.id]
  parameter_group_name    = aws_db_parameter_group.civiform.name
  publicly_accessible     = false
  skip_final_snapshot     = true
  backup_retention_period = var.postgres_backup_retention_days
}
