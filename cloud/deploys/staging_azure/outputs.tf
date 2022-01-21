output "app_service_default_hostname" {
  value = module.app.app_service_default_hostname
}

output "pg_server_privatelink_fqdn" {
  value = module.app.pg_server_privatelink_fqdn
}
