variable "region" {
  type        = string
  description = "Default region for the db"
}

variable "connection_name" {
    type    = string
    description = "DB connection string"
}

variable "bucket_name" {
    type = string
    description = "Name of the file storage bucket"
}

variable "http_port" {
    type = string
    description = "Port to open on container for HTTP requests"
}

variable "application_service_account_email" {
    type = string
    description = "The service account application runs with"
}

variable "secret_id" {
    type = string
    description = "Secret Manager secret id for database password"
}
