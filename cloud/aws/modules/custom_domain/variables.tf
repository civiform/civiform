variable "custom_route_zone" {
  type        = string
  description = "Route zone for Civiform (e.g. civiform.dev)"

}
variable "custom_subdomain" {
  type        = string
  description = "Custom domain for Civiform (e.g. staging-aws)."
}
variable "apprunner_arn" {
  type        = string
  description = "ARN for apprunner instance"
}
