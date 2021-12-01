output "app_service_default_hostname" {
  value = "https://${azurerm_app_service.civiform_app.default_site_hostname}"
}
