variable "aws_region" {
  type        = string
  description = "Region where the AWS servers will live"
  default     = "us-east-1"
}

variable "log_storage_bucket" {
  type        = string
  description = "Name for S3 bucket to store logs"
  default     = "civiform-aws-staging-log-bucket"
}

variable "backend_state_bucket" {
  type        = string
  description = "Name for S3 bucket to store backend state for TF"
  default     = "civiform-tfstate-bucket"
}

variable "lock_table_name" {
  type        = string
  description = "Name for DymanoDB table that handles locking of TF backend state"
  default     = "civiform-backend-lock-table-tfstate"
}
