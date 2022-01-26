resource "random_password" "password" {
  length           = 40
  special          = true
}


resource "google_secret_manager_secret" "database_password" {
  secret_id = "database-password"
  replication {
    automatic = true
  }
}

resource "google_secret_manager_secret_version" "database_password_version" {
  secret = google_secret_manager_secret.database_password.id
  secret_data = random_password.password.result
}

resource "google_sql_database_instance" "civiform_db" {
  name             = "civiform-instance"
  database_version = "POSTGRES_12"
  region           = var.region

  settings {
      ip_configuration {
      ipv4_enabled    = true
    }
    tier = var.tier_type
  }
}

## TODO(ktoor@google.com): Change this with the service account running the application.
resource "google_sql_user" "civiform-user" {
  name     = "ktoor@google.com"
  instance = google_sql_database_instance.civiform_db.name
  password = google_secret_manager_secret_version.database_password_version.secret_data
  type     = "CLOUD_IAM_USER"
}