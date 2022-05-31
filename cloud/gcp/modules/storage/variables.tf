variable "region" {
  type        = string
  description = "Default region for bucket resources"
}

variable "application_name_postfix" {
  type        = string
  description = "Company name to be used postfix"
}

variable "application_service_account_email" {
  type        = string
  description = "The service account application runs with"
}