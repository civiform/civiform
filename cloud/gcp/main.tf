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

module "IAM" {
  source = "./modules/IAM"
  terraform_service_account_email   = var.terraform_service_account
  project_id                        = var.project_id
}

module "network" {
  source = "./modules/network"
  region = var.region
}

module "storage" {
  source                            = "./modules/storage"
  region                            = var.region
  application_name_postfix          = var.application_name_postfix
  application_service_account_email = module.IAM.application_service_account_email
}

module "database" {
  source                            = "./modules/database"
  terraform_service_account         = var.terraform_service_account
  region                            = var.region
  tier_type                         = var.db_tier_type
  application_service_account_email = module.IAM.application_service_account_email
}

module "compute" {
  source                            = "./modules/compute"
  region                            = var.region
  http_port                         = var.http_port
  bucket_name                       = module.storage.bucket_name
  db_connection_name                = module.database.db_connection_name
  application_service_account_email = module.IAM.application_service_account_email
  db_secret_id                         = module.database.db_secret_id
  civiform_image_name               = var.civiform_image_name
  project_id                        = var.project_id
  terraform_service_account_email   = var.terraform_service_account
}

