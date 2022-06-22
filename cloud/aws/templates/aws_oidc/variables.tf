variable "aws_region" {
  type        = string
  description = "Region where the AWS servers will live"
  default     = "us-east-1"
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

variable "ses_sender_email" {
  type        = string
  description = "Email address that emails will be sent from"
  default     = ""
}

variable "staging_applicant_notification_mailing_list" {
  type        = string
  description = "Applicant notification mailing list for staging"
  default     = ""
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

variable "app_prefix" {
  type        = string
  description = "A prefix to add to values so we can have multiple deploys in the same aws account"
}