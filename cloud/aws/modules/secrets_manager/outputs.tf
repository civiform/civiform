output "app_secret_key" {
  value     = data.aws_secretsmanager_secret_version.app_secret_key_version.secret_string
  sensitive = true
}

output "database_username" {
  value     = data.aws_secretsmanager_secret_version.postgres_username_version.secret_string
  sensitive = true
}

output "database_password" {
  value     = data.aws_secretsmanager_secret_version.postgres_password_version.secret_string
  sensitive = true
}
