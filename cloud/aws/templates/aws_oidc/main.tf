locals {
  # Make db deletable on staging.
  deletion_protection = var.civiform_mode == "prod" ? true : false
  skip_final_snapshot = var.civiform_mode == "prod" ? false : true
  force_destroy_s3    = var.civiform_mode == "prod" ? false : true
}

# TODO: split this into modules.
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
  deletion_protection             = local.deletion_protection
  instance_class                  = var.postgres_instance_class
  allocated_storage               = var.postgres_storage_gb
  engine                          = "postgres"
  engine_version                  = "12"
  username                        = aws_secretsmanager_secret_version.postgres_username_secret_version.secret_string
  password                        = aws_secretsmanager_secret_version.postgres_password_secret_version.secret_string
  vpc_security_group_ids          = [aws_security_group.rds.id]
  db_subnet_group_name            = module.vpc.database_subnet_group_name
  parameter_group_name            = aws_db_parameter_group.civiform.name
  publicly_accessible             = false
  skip_final_snapshot             = local.skip_final_snapshot
  final_snapshot_identifier       = "${var.app_prefix}-civiform-db-finalsnapshot"
  backup_retention_period         = var.postgres_backup_retention_days
  kms_key_id                      = aws_kms_key.civiform_kms_key.arn
  storage_encrypted               = true
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
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
