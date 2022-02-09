variable "vnet_name" {
  type        = string
  description = "the app vnet name"
}

variable "resource_group_name" {
  type        = string
  description = "name of the resource group"
}

variable "resource_group_location" {
  type        = string
  description = "location for the resource group"
}

variable "bastion_address_prefixes" {
  type        = list(string)
  description = "Prefixes for the bastion instance"
  default = [
    "10.0.6.0/24"
  ]
}
