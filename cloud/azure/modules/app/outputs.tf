output "app_service_default_hostname" {
  value = "https://${azurerm_app_service.civiform_app.default_site_hostname}"
}

output "app_service_canary_hostname" {
  value = "https://${azurerm_app_service_slot.canary.default_site_hostname}"
}
output "pg_server_privatelink_fqdn" {
  value = local.postgres_private_link
}

output "app_service_name" {
  value = azurerm_app_service.civiform_app.name
}

output "resource_group_name" {
  value = data.azurerm_resource_group.rg.name
}
