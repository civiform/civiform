provider "google" {
  alias = "impersonation"
  scopes = [
    "https://www.googleapis.com/auth/cloud-platform",
    "https://www.googleapis.com/auth/userinfo.email",
  ]
}

data "google_service_account_access_token" "default" {
  provider               = google.impersonation
  target_service_account = var.terraform_service_account
  scopes                 = ["userinfo-email", "cloud-platform"]
  lifetime               = "1200s"
}

provider "google" {
  project         = "civiform-demo"
  access_token    = data.google_service_account_access_token.default.access_token
  request_timeout = "60s"
}

module "network" {
  source = "./network"
  region = var.region
}

module "storage" {
  source           = "./storage"
  region           = var.region
  application_name = var.application_name
}

