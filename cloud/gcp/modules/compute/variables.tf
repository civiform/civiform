variable "region" {
  type        = string
  description = "Default region for the application"
}

variable "db_connection_name" {
  type        = string
  description = "DB connection string"
}

variable "bucket_name" {
  type        = string
  description = "Name of the file storage bucket"
}

variable "http_port" {
  type        = string
  description = "Port to open on container for HTTP requests"
}

variable "application_service_account_email" {
  type        = string
  description = "The service account application runs with"
}

variable "db_secret_id" {
  type        = string
  description = "Secret Manager secret id for database password"
}

variable "civiform_image_name" {
  type        = string
  description = "Suffix for the latest civiform image. For example, `civform-latest`."
}

variable "project_id" {
  type        = string
  description = "GCP project id name for the civform project."
}

variable "terraform_service_account_email" {
  type        = string
  description = "service account that runs terraform."
}

