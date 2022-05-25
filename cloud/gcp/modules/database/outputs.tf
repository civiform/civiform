output "connection_name" {
  value = google_sql_database_instance.civiform_db.connection_name
}

output "db-ip" {
  value = google_sql_database_instance.civiform_db.ip_address.0.ip_address
}
