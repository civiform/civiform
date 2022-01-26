variable "terraform_service_account" {
  type        = string
  description = "Service Account to use for running terraform on GCP"
}

variable "region" {
  type        = string
  default     = "us-west1"
  description = "Default region for the project"
}

variable "application_name_postfix" {
  type        = string
  default     = "civiform"
  description = "application name to be used as postfix to resources"
}

variable "db_tier_type" {
  type = string
  description = "vm tier type to run db instance"
  default = "db-n1-standard-4"
}
