# Importing the AWS secrets created previously using name.
data "aws_secretsmanager_secret" "postgres_username" {
  name = "${var.app_prefix}-postgres_username"
}

# Importing the AWS secret version created previously using arn.
data "aws_secretsmanager_secret_version" "postgres_username_version" {
  secret_id = data.aws_secretsmanager_secret.postgres_username.arn
}

# Importing the AWS secrets created previously using name.
data "aws_secretsmanager_secret" "postgres_password" {
  name = "${var.app_prefix}-postgres_password"
}

# Importing the AWS secret version created previously using arn.
data "aws_secretsmanager_secret_version" "postgres_password_version" {
  secret_id = data.aws_secretsmanager_secret.postgres_password.arn
}

# Importing the AWS secrets created previously using name.
data "aws_secretsmanager_secret" "app_secret_key" {
  name = "${var.app_prefix}-app_secret_key"
}

# Importing the AWS secret version created previously using arn.
data "aws_secretsmanager_secret_version" "app_secret_key_version" {
  secret_id = data.aws_secretsmanager_secret.app_secret_key.arn
}

# Importing the AWS secrets created previously using name.
data "aws_secretsmanager_secret" "adfs_secret" {
  name = "${var.app_prefix}-adfs_secret"
}

# Importing the AWS secret version created previously using arn.
data "aws_secretsmanager_secret_version" "adfs_secret_version" {
  secret_id = data.aws_secretsmanager_secret.adfs_secret.arn
}
