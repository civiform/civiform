locals {
  # Make secrets immediately deletable on 'staging' and 'dev' for easy
  # development iteration. By default secrets are not deleted immediately and
  # instead scheduled for deletion in 30 days. Whey they are not deleted
  # immediately - you can't destroy and re-create environment from scratch
  # without force-deleting secrets using aws cli.
  secret_recovery_window_in_days = var.civiform_mode == "prod" ? 30 : 0
}

# Create a KMS key to encrypt the secrets. Note this
# isn't totally necessary, but allows for custom IAM
# policies on the key
resource "aws_kms_key" "civiform_kms_key" {
  tags = {
    Name = "${var.app_prefix} Civiform KMS Key"
    Type = "Civiform KMS Key"
  }
  description             = "KMS key for civiform"
  deletion_window_in_days = 10
  enable_key_rotation     = true
}

# Create a random generated password to use for postgres_password.
resource "random_password" "postgres_username" {
  length  = 7
  special = false
  keepers = {
    version = 1
  }
}

# Creating a AWS secret for postgres_username
resource "aws_secretsmanager_secret" "postgres_username_secret" {
  tags = {
    Name = "${var.app_prefix} Civiform Postgres Username Secret"
    Type = "Civiform Postgres Username Secret"
  }
  name                    = "${var.app_prefix}-postgres_username"
  kms_key_id              = aws_kms_key.civiform_kms_key.arn
  recovery_window_in_days = local.secret_recovery_window_in_days
}

# Creating a AWS secret versions for postgres_username
resource "aws_secretsmanager_secret_version" "postgres_username_secret_version" {
  secret_id     = aws_secretsmanager_secret.postgres_username_secret.id
  secret_string = "db_admin_${random_password.postgres_username.result}"
}

# Create a random generated password to use for postgres_password.
resource "random_password" "postgres_password" {
  length           = 40
  special          = true
  min_special      = 5
  override_special = "!#$%^&*()-_=+[]{}<>:?"
  keepers = {
    version = 1
  }
}

# Creating a AWS secret for postgres_password
resource "aws_secretsmanager_secret" "postgres_password_secret" {
  tags = {
    Name = "${var.app_prefix} Civiform Postgres Password Secret"
    Type = "Civiform Postgres Password Secret"
  }
  name                    = "${var.app_prefix}-postgres_password"
  kms_key_id              = aws_kms_key.civiform_kms_key.arn
  recovery_window_in_days = local.secret_recovery_window_in_days
}

# Creating a AWS secret versions for postgres_password
resource "aws_secretsmanager_secret_version" "postgres_password_secret_version" {
  secret_id = aws_secretsmanager_secret.postgres_password_secret.id
  # Prefix secret value with 'default-' so that we can detect it in the
  # deployment script and regenerate. See
  # Setup._maybe_change_default_db_password() in aws_oidc/bin/setup.py
  secret_string = "default-${random_password.postgres_password.result}"
}

# Create a random generated password to use for app_secret_key.
resource "random_password" "app_secret_key" {
  length           = 16
  special          = true
  override_special = "_%@"
}

# Creating a AWS secret for app_secret_key
resource "aws_secretsmanager_secret" "app_secret_key_secret" {
  tags = {
    Name = "${var.app_prefix} Civiform App Secret Secret"
    Type = "Civiform App Secret Secret"
  }
  name                    = "${var.app_prefix}-app_secret_key"
  recovery_window_in_days = local.secret_recovery_window_in_days
}

# Creating a AWS secret versions for app_secret_key
resource "aws_secretsmanager_secret_version" "app_secret_key_secret_version" {
  secret_id     = aws_secretsmanager_secret.app_secret_key_secret.id
  secret_string = random_password.app_secret_key.result
}

# Create a random generated password to use for api_secret_salt.
resource "random_password" "api_secret_salt" {
  length           = 16
  special          = true
  override_special = "_%@"
}

# Creating a AWS secret for api_secret_salt
resource "aws_secretsmanager_secret" "api_secret_salt_secret" {
  tags = {
    Name = "${var.app_prefix} Civiform Api Secret Salt Secret"
    Type = "Civiform Api Secret Salt Secret"
  }
  name                    = "${var.app_prefix}-api_secret_salt"
  recovery_window_in_days = local.secret_recovery_window_in_days
}

# Creating a AWS secret versions for api_secret_salt
resource "aws_secretsmanager_secret_version" "api_secret_salt_secret_version" {
  secret_id     = aws_secretsmanager_secret.api_secret_salt_secret.id
  secret_string = random_password.api_secret_salt.result
}

# Creating a AWS secret for adfs_secret
resource "aws_secretsmanager_secret" "adfs_secret_secret" {
  tags = {
    Name = "${var.app_prefix} Civiform ADFS Secret Secret"
    Type = "Civiform ADFS Secret Secret"
  }
  name                    = "${var.app_prefix}-adfs_secret"
  kms_key_id              = aws_kms_key.civiform_kms_key.arn
  recovery_window_in_days = local.secret_recovery_window_in_days
}

# Creating a AWS secret versions for adfs_secret
resource "aws_secretsmanager_secret_version" "adfs_secret_secret_version" {
  secret_id     = aws_secretsmanager_secret.adfs_secret_secret.id
  secret_string = " "
}

# Creating a AWS secret for adfs_client_id
resource "aws_secretsmanager_secret" "adfs_client_id_secret" {
  tags = {
    Name = "${var.app_prefix} Civiform ADFS Client ID Secret"
    Type = "Civiform ADFS Client ID Secret"
  }
  name                    = "${var.app_prefix}-adfs_client_id"
  kms_key_id              = aws_kms_key.civiform_kms_key.arn
  recovery_window_in_days = local.secret_recovery_window_in_days
}

# Creating a AWS secret versions for adfs_client_id
resource "aws_secretsmanager_secret_version" "adfs_client_id_secret_version" {
  secret_id     = aws_secretsmanager_secret.adfs_client_id_secret.id
  secret_string = " "
}

# Creating a AWS secret for applicant_oidc_secret
resource "aws_secretsmanager_secret" "applicant_oidc_client_secret_secret" {
  name                    = "${var.app_prefix}-applicant_oidc_client_secret"
  kms_key_id              = aws_kms_key.civiform_kms_key.arn
  recovery_window_in_days = local.secret_recovery_window_in_days
}

# Creating a AWS secret versions for applicant_oidc_secret
resource "aws_secretsmanager_secret_version" "applicant_oidc_client_secret_secret_version" {
  secret_id     = aws_secretsmanager_secret.applicant_oidc_client_secret_secret.id
  secret_string = " "
}

# Creating a AWS secret for applicant_oidc_client_id
resource "aws_secretsmanager_secret" "applicant_oidc_client_id_secret" {
  name                    = "${var.app_prefix}-applicant_oidc_client_id"
  kms_key_id              = aws_kms_key.civiform_kms_key.arn
  recovery_window_in_days = local.secret_recovery_window_in_days
}

# Creating a AWS secret versions for applicant_oidc_client_id
resource "aws_secretsmanager_secret_version" "applicant_oidc_client_id_secret_version" {
  secret_id     = aws_secretsmanager_secret.applicant_oidc_client_id_secret.id
  secret_string = " "
}
