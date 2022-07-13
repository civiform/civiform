variable "app_prefix" {
  type        = string
  description = "A prefix to add to values so we can have multiple deploys in the same aws account"
}
variable "civiform_mode" {
  type        = string
  description = "The civiform environment mode (test/dev/staging/prod)"
}

