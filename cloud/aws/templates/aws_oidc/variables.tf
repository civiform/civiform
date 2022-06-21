variable "aws_region" {
  type        = string
  description = "Region where the AWS servers will live"
  default     = "us-east-1"
}

variable "civiform_image_repo" {
  type        = string
  description = "Public ECR repository with Civiform images"
  default     = "public.ecr.aws/t1q6b4h2/universal-application-tool"
}

variable "civiform_image_tag" {
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

variable "vpc_name" {
  type        = string
  description = "Name of the VPC for the database"
  default     = "civiform_rds_vpc"
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
