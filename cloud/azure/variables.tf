variable "application_name" {
  type        = string
  description = "Azure Web App Name"
}

variable "docker_username" {
  type        = string
  description = "Docker username"
}

variable "docker_repository_name" {
  type        = string
  description = "Name of container image"
}

variable "image_tag_name" {
  type        = string
  description = "Tag for container image"
  default     = "latest"
}
