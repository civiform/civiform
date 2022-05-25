resource "azurerm_log_analytics_workspace" "civiform_logs" {
  name                = "civiform-server-logs"
  location            = data.azurerm_resource_group.rg.location
  resource_group_name = data.azurerm_resource_group.rg.name
  sku                 = var.log_sku
  retention_in_days   = var.log_retention
}

resource "azurerm_monitor_diagnostic_setting" "app_service_log_analytics" {
  name                       = "${var.application_name}_log_analytics"
  target_resource_id         = azurerm_app_service.civiform_app.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.civiform_logs.id

  log {
    category = "AppServiceAppLogs"

    retention_policy {
      days    = 0
      enabled = false
    }
  }

  log {
    category = "AppServiceConsoleLogs"

    retention_policy {
      days    = 0
      enabled = false
    }
  }

  log {
    category = "AppServiceHTTPLogs"

    retention_policy {
      days    = 0
      enabled = false
    }
  }

  log {
    category = "AppServiceAuditLogs"

    retention_policy {
      days    = 0
      enabled = false
    }
  }
  metric {
    category = "AllMetrics"

    retention_policy {
      days    = 0
      enabled = false
    }
  }

  # due to a bug in terraform include these even though they are not enabled
  log {
    category = "AppServiceIPSecAuditLogs"
    enabled  = false

    retention_policy {
      days    = 0
      enabled = false
    }
  }
  log {
    category = "AppServicePlatformLogs"
    enabled  = false

    retention_policy {
      days    = 0
      enabled = false
    }
  }
}
