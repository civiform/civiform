# Firstly create a random generated password to use in secrets.
resource "random_password" "postgres_password" {
  length           = 16
  special          = true
  override_special = "_%@"
}

# Creating a AWS secret for postgres_password
resource "aws_secretsmanager_secret" "postgres_password_secret" {
  name = "postgres_password"
}

# Creating a AWS secret versions fro postgres_password
resource "aws_secretsmanager_secret_version" "postgres_password_secret_version" {
  secret_id     = aws_secretsmanager_secret.postgres_password_secret.id
  secret_string = random_password.postgres_password.result
}

# Firstly create a random generated password to use in secrets.
resource "random_password" "app_secret_key" {
  length           = 16
  special          = true
  override_special = "_%@"
}

# Creating a AWS secret for app_secret_key
resource "aws_secretsmanager_secret" "app_secret_key_secret" {
  name = "app_secret_key"
}

# Creating a AWS secret versions fro app_secret_key
resource "aws_secretsmanager_secret_version" "app_secret_key_secret_version" {
  secret_id     = aws_secretsmanager_secret.app_secret_key_secret.id
  secret_string = random_password.app_secret_key.result
}

# Creating a AWS secret for adfs_secret
resource "aws_secretsmanager_secret" "adfs_secret_secret" {
  name = "adfs_secret"
}

# Creating a AWS secret versions fro adfs_secret
resource "aws_secretsmanager_secret_version" "adfs_secret_secret_version" {
  secret_id     = aws_secretsmanager_secret.adfs_secret_secret.id
  secret_string = " "
}

# Creating a AWS secret for aws_secret_access_token
resource "aws_secretsmanager_secret" "aws_secret_access_token_secret" {
  name = "aws_secret_access_token"
}

# Creating a AWS secret versions fro aws_secret_access_token
resource "aws_secretsmanager_secret_version" "aws_secret_access_token_secret_version" {
  secret_id     = aws_secretsmanager_secret.aws_secret_access_token_secret.id
  secret_string = "CHANGE_ME"
}

# Creating a AWS secret for aws_access_key_id
resource "aws_secretsmanager_secret" "aws_access_key_id_secret" {
  name = "aws_access_key_id"
}

# Creating a AWS secret versions fro aws_access_key_id
resource "aws_secretsmanager_secret_version" "aws_access_key_id_secret_version" {
  secret_id     = aws_secretsmanager_secret.aws_access_key_id_secret.id
  secret_string = "CHANGE_ME"
}

# Creating a AWS secret for adfs_client_id
resource "aws_secretsmanager_secret" "adfs_client_id_secret" {
  name = "adfs_client_id"
}

# Creating a AWS secret versions fro adfs_client_id
resource "aws_secretsmanager_secret_version" "adfs_client_id_secret_version" {
  secret_id     = aws_secretsmanager_secret.adfs_client_id_secret.id
  secret_string = " "
}

# Creating a AWS secret for adfs_discovery_uri
resource "aws_secretsmanager_secret" "adfs_discovery_uri_secret" {
  name = "adfs_discovery_uri"
}

# Creating a AWS secret versions fro adfs_discovery_uri
resource "aws_secretsmanager_secret_version" "adfs_discovery_uri_secret_version" {
  secret_id     = aws_secretsmanager_secret.adfs_discovery_uri_secret.id
  secret_string = " "
}
