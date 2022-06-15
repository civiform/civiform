# Importing the AWS secrets created previously using name.
data "aws_secretsmanager_secret" "postgres_username" {
  name = "postgres_username"
}

# Importing the AWS secret version created previously using arn.
data "aws_secretsmanager_secret_version" "postgres_username_version" {
  secret_id = data.aws_secretsmanager_secret.postgres_username.arn
}

# Importing the AWS secrets created previously using name.
data "aws_secretsmanager_secret" "postgres_password" {
  name = "postgres_password"
}

# Importing the AWS secret version created previously using arn.
data "aws_secretsmanager_secret_version" "postgres_password_version" {
  secret_id = data.aws_secretsmanager_secret.postgres_password.arn
}

# Importing the AWS secrets created previously using name.
data "aws_secretsmanager_secret" "aws_secret_access_token" {
  name = "aws_secret_access_token"
}

# Importing the AWS secret version created previously using arn.
data "aws_secretsmanager_secret_version" "aws_secret_access_token_version" {
  secret_id = data.aws_secretsmanager_secret.aws_secret_access_token.arn
}

# Importing the AWS secrets created previously using name.
data "aws_secretsmanager_secret" "aws_access_key_id" {
  name = "aws_access_key_id"
}

# Importing the AWS secret version created previously using arn.
data "aws_secretsmanager_secret_version" "aws_access_key_id_version" {
  secret_id = data.aws_secretsmanager_secret.aws_access_key_id.arn
}

# Importing the AWS secrets created previously using name.
data "aws_secretsmanager_secret" "app_secret_key" {
  name = "app_secret_key"
}

# Importing the AWS secret version created previously using arn.
data "aws_secretsmanager_secret_version" "app_secret_key_version" {
  secret_id = data.aws_secretsmanager_secret.app_secret_key.arn
}

# Importing the AWS secrets created previously using name.
data "aws_secretsmanager_secret" "adfs_secret" {
  name = "adfs_secret"
}

# Importing the AWS secret version created previously using arn.
data "aws_secretsmanager_secret_version" "adfs_secret_version" {
  secret_id = data.aws_secretsmanager_secret.adfs_secret.arn
}
