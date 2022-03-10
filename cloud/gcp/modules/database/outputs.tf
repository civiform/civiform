output "db_connection_name" {
    value = google_sql_database_instance.civiform_db.connection_name
}

output "db_ip" {
    value = google_sql_database_instance.civiform_db.ip_address.0.ip_address
}

output "db_secret_id" {
    value = google_secret_manager_secret.database_password.secret_id
}