# Create a KMS key to encrypt the secrets. Note this
# isn't totally necessary, but allows for custom IAM
# policies on the key
resource "aws_kms_key" "civiform_kms_key" {
  description             = "KMS key for civiform"
  deletion_window_in_days = 10
}

# Create a random generated password to use for postgres_password.
resource "random_password" "postgres_username" {
  length           = 16
  special          = true
  override_special = "_%@"
}

# Creating a AWS secret for postgres_username
resource "aws_secretsmanager_secret" "postgres_username_secret" {
  name       = "${var.app_prefix}-postgres_username"
  kms_key_id = aws_kms_key.civiform_kms_key.arn
}

# Creating a AWS secret versions for postgres_username
resource "aws_secretsmanager_secret_version" "postgres_username_secret_version" {
  secret_id     = aws_secretsmanager_secret.postgres_username_secret.id
  secret_string = random_password.postgres_username.result
}

# Create a random generated password to use for postgres_password.
resource "random_password" "postgres_password" {
  length  = 16
  special = false
}

# Creating a AWS secret for postgres_password
resource "aws_secretsmanager_secret" "postgres_password_secret" {
  name       = "${var.app_prefix}-postgres_password"
  kms_key_id = aws_kms_key.civiform_kms_key.arn
}

# Creating a AWS secret versions for postgres_password
resource "aws_secretsmanager_secret_version" "postgres_password_secret_version" {
  secret_id     = aws_secretsmanager_secret.postgres_password_secret.id
  secret_string = random_password.postgres_password.result
}

# Create a random generated password to use for app_secret_key.
resource "random_password" "app_secret_key" {
  length           = 16
  special          = true
  override_special = "_%@"
}

# Creating a AWS secret for app_secret_key
resource "aws_secretsmanager_secret" "app_secret_key_secret" {
  name = "${var.app_prefix}-app_secret_key"
}

# Creating a AWS secret versions for app_secret_key
resource "aws_secretsmanager_secret_version" "app_secret_key_secret_version" {
  secret_id     = aws_secretsmanager_secret.app_secret_key_secret.id
  secret_string = random_password.app_secret_key.result
}

# Creating a AWS secret for adfs_secret
resource "aws_secretsmanager_secret" "adfs_secret_secret" {
  name       = "${var.app_prefix}-adfs_secret"
  kms_key_id = aws_kms_key.civiform_kms_key.arn
}

# Creating a AWS secret versions for adfs_secret
resource "aws_secretsmanager_secret_version" "adfs_secret_secret_version" {
  secret_id     = aws_secretsmanager_secret.adfs_secret_secret.id
  secret_string = " "
}

# Creating a AWS secret for adfs_client_id
resource "aws_secretsmanager_secret" "adfs_client_id_secret" {
  name       = "${var.app_prefix}-adfs_client_id"
  kms_key_id = aws_kms_key.civiform_kms_key.arn
}

# Creating a AWS secret versions for adfs_client_id
resource "aws_secretsmanager_secret_version" "adfs_client_id_secret_version" {
  secret_id     = aws_secretsmanager_secret.adfs_client_id_secret.id
  secret_string = " "
}

# Creating a AWS secret for adfs_discovery_uri
resource "aws_secretsmanager_secret" "adfs_discovery_uri_secret" {
  name       = "${var.app_prefix}-adfs_discovery_uri"
  kms_key_id = aws_kms_key.civiform_kms_key.arn
}

# Creating a AWS secret versions for adfs_discovery_uri
resource "aws_secretsmanager_secret_version" "adfs_discovery_uri_secret_version" {
  secret_id     = aws_secretsmanager_secret.adfs_discovery_uri_secret.id
  secret_string = " "
}
