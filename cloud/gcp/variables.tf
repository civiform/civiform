variable "terraform_service_account" {
  type        = string
  description = "Service Account to use for running terraform on GCP"
}

variable "region" {
  type        = string
  default     = "us-west1"
  description = "Default region for the project"
}

variable "application_name" {
  type        = string
  default     = "civform"
  description = "application name to be used as postfix to resources"
}
