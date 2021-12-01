output "app_service_default_hostname" {
  value = "https://${azurerm_app_service.civiform_app.default_site_hostname}"
}

output "pg_server_privatelink_fqdn" {
  value = azurerm_private_endpoint.endpoint.private_dns_zone_configs[0].record_sets[0].fqdn
}
