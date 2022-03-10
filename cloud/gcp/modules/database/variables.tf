variable "region" {
  type        = string
  description = "Default region for the db"
}

variable "tier_type" {
    type = string
    description = "tier type to use for the db instance"
}

variable "terraform_service_account" {
    type = string
    description = "service account being used by terraform"
}

variable "application_service_account_email" {
    type = string 
    description = "The service account application runs with"
}
