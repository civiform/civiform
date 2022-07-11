# TODO: split this into modules.
module "secrets" {
  source     = "../../modules/secrets_manager"
  app_prefix = var.app_prefix
}

resource "aws_apprunner_service" "civiform_dev" {
  tags = {
    Name = "${var.app_prefix} Civiform Server"
    Type = "Civiform Server"
  }
  auto_scaling_configuration_arn = aws_apprunner_auto_scaling_configuration_version.auto_scaling_config.arn
  service_name                   = "${var.app_prefix}-civiform_dev"

  source_configuration {
    image_repository {
      image_configuration {
        port = var.port

        runtime_environment_variables = {
          SECRET_KEY = module.secrets.app_secret_key
          PORT       = var.port

          DB_JDBC_STRING = "jdbc:postgresql://${aws_db_instance.civiform.address}:${aws_db_instance.civiform.port}/postgres?ssl=true&sslmode=require"
          DB_USERNAME    = aws_db_instance.civiform.username
          DB_PASSWORD    = aws_db_instance.civiform.password

          STAGING_HOSTNAME = var.staging_hostname
          BASE_URL         = var.base_url != "" ? var.base_url : var.custom_hostname

          STORAGE_SERVICE_NAME = "s3"
          AWS_S3_BUCKET_NAME   = aws_s3_bucket.civiform_files_s3.id

          CIVIFORM_TIME_ZONE_ID              = var.civiform_time_zone_id
          WHITELABEL_CIVIC_ENTITY_SHORT_NAME = var.civic_entity_short_name
          WHITELABEL_CIVIC_ENTITY_FULL_NAME  = var.civic_entity_full_name
          WHITELABEL_SMALL_LOGO_URL          = var.civic_entity_small_logo_url
          WHITELABEL_LOGO_WITH_NAME_URL      = var.civic_entity_logo_with_name_url
          SUPPORT_EMAIL_ADDRESS              = var.civic_entity_support_email_address

          AWS_SES_SENDER = var.ses_sender_email
          AWS_REGION     = var.aws_region

          STAGING_ADMIN_LIST           = var.staging_program_admin_notification_mailing_list
          STAGING_TI_LIST              = var.staging_ti_notification_mailing_list
          STAGING_APPLICANT_LIST       = var.staging_applicant_notification_mailing_list
          APPLICANT_OIDC_PROVIDER_NAME = var.applicant_oidc_provider_name
          CIVIFORM_APPLICANT_IDP       = var.civiform_applicant_idp
          APPLICANT_OIDC_CLIENT_ID     = var.applicant_oidc_client_id
          APPLICANT_OIDC_CLIENT_SECRET = var.applicant_oidc_client_secret
          APPLICANT_OIDC_DISCOVERY_URI = var.applicant_oidc_discovery_uri
          // TODO Switch to use secrets
          # APPLICANT_OIDC_CLIENT_ID         = module.secrets.applicant_oidc_client_id
          # APPLICANT_OIDC_CLIENT_SECRET     = module.secrets.applicant_oidc_client_secret
          # APPLICANT_OIDC_DISCOVERY_URI     = module.secrets.applicant_oidc_discovery_uri
          APPLICANT_OIDC_RESPONSE_MODE         = var.applicant_oidc_response_mode
          APPLICANT_OIDC_RESPONSE_TYPE         = var.applicant_oidc_response_type
          APPLICANT_OIDC_ADDITIONAL_SCOPES     = var.applicant_oidc_additional_scopes
          APPLICANT_OIDC_LOCALE_ATTRIBUTE      = var.applicant_oidc_locale_attribute
          APPLICANT_OIDC_EMAIL_ATTRIBUTE       = var.applicant_oidc_email_attribute
          APPLICANT_OIDC_FIRST_NAME_ATTRIBUTE  = var.applicant_oidc_first_name_attribute
          APPLICANT_OIDC_MIDDLE_NAME_ATTRIBUTE = var.applicant_oidc_middle_name_attribute
          APPLICANT_OIDC_LAST_NAME_ATTRIBUTE   = var.applicant_oidc_last_name_attribute
        }
      }

      image_identifier      = "${var.civiform_image_repo}:${var.image_tag}"
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

# List of params that we could configure:
# https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Appendix.PostgreSQL.CommonDBATasks.Parameters.html#Appendix.PostgreSQL.CommonDBATasks.Parameters.parameters-list
resource "aws_db_parameter_group" "civiform" {
  name = "${var.app_prefix}-civiform-db-params"
  tags = {
    Name = "${var.app_prefix} Civiform DB Parameters"
    Type = "Civiform DB Parameters"
  }

  family = "postgres12"

  parameter {
    name  = "log_connections"
    value = "1"
  }
}

resource "aws_db_instance" "civiform" {
  identifier = "${var.app_prefix}-${var.postgress_name}-db"
  tags = {
    Name = "${var.app_prefix} Civiform Database"
    Type = "Civiform Database"
  }

  instance_class          = var.postgres_instance_class
  allocated_storage       = var.postgres_storage_gb
  engine                  = "postgres"
  engine_version          = "12"
  username                = module.secrets.database_username
  password                = module.secrets.database_password
  vpc_security_group_ids  = [aws_security_group.rds.id]
  db_subnet_group_name    = module.vpc.database_subnet_group_name
  parameter_group_name    = aws_db_parameter_group.civiform.name
  publicly_accessible     = false
  skip_final_snapshot     = true
  backup_retention_period = var.postgres_backup_retention_days
}

module "email_service" {
  for_each = toset([
    var.sender_email_address,
    var.staging_applicant_notification_mailing_list,
    var.staging_ti_notification_mailing_list,
    var.staging_program_admin_notification_mailing_list
  ])
  source               = "../../modules/ses"
  sender_email_address = each.key
}
