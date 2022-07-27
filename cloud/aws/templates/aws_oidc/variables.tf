variable "aws_region" {
  type        = string
  description = "Region where the AWS servers will live"
  default     = "us-east-1"
}

variable "civiform_image_repo" {
  type        = string
  description = "Dockerhub repository with Civiform images"
  default     = "civiform/civiform"
}

variable "image_tag" {
  type        = string
  description = "Image tag of the Civiform docker image to deploy"
  default     = "prod"
}

variable "civiform_time_zone_id" {
  type        = string
  description = "Time zone for Civiform server to use when displaying dates."
  default     = "America/Los_Angeles"
}

variable "civic_entity_short_name" {
  type        = string
  description = "Short name for civic entity (example: Rochester, Seattle)."
  default     = "Dev Civiform"
}

variable "civic_entity_full_name" {
  type        = string
  description = "Full name for civic entity (example: City of Rochester, City of Seattle)."
  default     = "City of Dev Civiform"
}

variable "civic_entity_support_email_address" {
  type        = string
  description = "Email address where applicants can contact civic entity for support with Civiform."
  default     = "azizoval@google.com"
}

variable "civic_entity_logo_with_name_url" {
  type        = string
  description = "Logo with name used on the applicant-facing program index page"
  default     = "https://raw.githubusercontent.com/civiform/staging-azure-deploy/main/logos/civiform-staging-long.png"
}

variable "civic_entity_small_logo_url" {
  type        = string
  description = "Logo with name used on the applicant-facing program index page"
  default     = "https://raw.githubusercontent.com/civiform/staging-azure-deploy/main/logos/civiform-staging.png"
}

variable "favicon_url" {
  type        = string
  description = "Browser Favicon (16x16 or 32x32 pixels, .ico, .png, or .gif) used on all pages"
  default     = "https://civiform.us/favicon.png"
}

variable "vpc_name" {
  type        = string
  description = "Name of the VPC"
  default     = "civiform-vpc"
}

variable "vpc_cidr" {
  type        = string
  description = "Cidr for VPC"
  default     = "10.0.0.0/16"
}

variable "private_subnets" {
  type        = list(string)
  description = "List of the private subnets for the VPC"
  default     = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
}

variable "database_subnets" {
  type        = list(string)
  description = "List of the database subnets for the VPC"
  default     = ["10.0.21.0/24", "10.0.22.0/24", "10.0.23.0/24"]
}

variable "public_subnets" {
  type        = list(string)
  description = "List of the public subnets for the VPC"
  default     = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]
}

# TODO: determine reasonable max concurrency for a civiform server
variable "auto_scaling_config" {
  type        = map(string)
  description = "Autoscaling config for AppRunner"
  default = {
    max_concurrency = "100",
    max_size        = "10",
    min_size        = "1"
  }
}

variable "postgress_name" {
  type        = string
  description = "Name for Postress DB"
  default     = "civiform"
}

variable "postgres_instance_class" {
  type        = string
  description = "The instance class for postgres server"
  default     = "db.t3.micro"
}

variable "postgres_storage_gb" {
  type        = number
  description = "The gb of storage for postgres instance"
  default     = 5
}

variable "postgres_backup_retention_days" {
  type        = number
  description = "Number of days to retain postgres backup"
  default     = 7
}

variable "staging_program_admin_notification_mailing_list" {
  type        = string
  description = "Admin notification mailing list for staging"
  default     = ""
}

variable "staging_ti_notification_mailing_list" {
  type        = string
  description = "intermediary notification mailing list for staging"
  default     = ""
}

variable "sender_email_address" {
  type        = string
  description = "Email address that emails will be sent from"
  default     = ""
}

variable "staging_applicant_notification_mailing_list" {
  type        = string
  description = "Applicant notification mailing list for staging"
  default     = ""
}

variable "file_storage_bucket" {
  type        = string
  description = "Name for S3 bucket to store files"
  default     = "civiform-files-s3"
}

variable "log_storage_bucket" {
  type        = string
  description = "Name for S3 bucket to store logs"
  default     = "civiform-aws-staging-log-bucket"
}

variable "ses_sender_email" {
  type        = string
  description = "Email address that emails will be sent from"
  default     = "azizoval@google.com"
}

variable "app_prefix" {
  type        = string
  description = "A prefix to add to values so we can have multiple deploys in the same aws account"
}

variable "applicant_oidc_provider_name" {
  type        = string
  description = "Applicant OIDC login provider name"
  default     = ""
}

variable "applicant_oidc_response_mode" {
  type        = string
  description = "Applicant OIDC login response mode"
  default     = ""
}

variable "applicant_oidc_response_type" {
  type        = string
  description = "Applicant OIDC login response type"
  default     = ""
}

variable "applicant_oidc_additional_scopes" {
  type        = string
  description = "Applicant OIDC login additional scopes to request"
  default     = ""
}

variable "applicant_oidc_locale_attribute" {
  type        = string
  description = "Applicant OIDC login user locale returned in token"
  default     = ""
}

variable "applicant_oidc_email_attribute" {
  type        = string
  description = "Applicant OIDC login user email returned in token"
  default     = ""
}

variable "applicant_oidc_first_name_attribute" {
  type        = string
  description = "Applicant OIDC login first name (or display name) returned in token"
  default     = ""
}

variable "applicant_oidc_middle_name_attribute" {
  type        = string
  description = "Applicant OIDC login middle name (if not using display name) returned in token"
  default     = ""
}

variable "applicant_oidc_last_name_attribute" {
  type        = string
  description = "Applicant OIDC login last name (if not using display name) returned in token"
  default     = ""
}

variable "civiform_applicant_idp" {
  type        = string
  description = "Applicant IDP"
  default     = ""
}

variable "applicant_oidc_client_id" {
  type        = string
  description = "Client ID"
  default     = ""
}

variable "applicant_oidc_client_secret" {
  type        = string
  description = "Client Secret"
  default     = ""
}

variable "applicant_oidc_discovery_uri" {
  type        = string
  description = "Discovery URI"
  default     = ""
}

variable "custom_hostname" {
  type        = string
  description = "The custom hostname this app is deployed on"
  default     = "staging-aws.civiform.dev"
}

variable "staging_hostname" {
  type        = string
  description = "If provided will enable DEMO mode on this hostname"
  default     = "staging-aws.civiform.dev"
}

variable "base_url" {
  type        = string
  description = "Base url for the app, only need to set if you don't have a custom hostname setup"
  default     = ""
}

variable "port" {
  type        = string
  description = "Port the app is running on"
  default     = "9000"
}

variable "civiform_mode" {
  type        = string
  description = "The civiform environment mode (test/dev/staging/prod)"
}
