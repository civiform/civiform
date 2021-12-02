output "app_service_default_hostname" {
  value = "https://${azurerm_app_service.civiform_app.default_site_hostname}"
}

output "pg_server_privatelink_fqdn" {
  value = local.postgres_private_link
}
