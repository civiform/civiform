variable "custom_hostname" {
  type        = string
  description = "custom hostname for the app to map the dns (used also for CORS)"
}

variable "app_service_name" {
  type        = string
  description = "the app service name"
}

variable "resource_group_name" {
  type        = string
  description = "name of the resource group"
}
