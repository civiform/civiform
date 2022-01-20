variable "terraform_service_account" {
  type        = string
  description = "Service Account to use for running terroform on GCP."
}

variable "region" {
  type        = string
  default     = "us-west1"
  description = "Default region for the project"
}

variable "company-name" {
    type = string
    default = "civform"
    description = "Company name to be used as prefix or postfix"
}
