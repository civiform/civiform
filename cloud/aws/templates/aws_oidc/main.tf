# TODO: split this into modules.

module "apprunner" {
    source = "../../modules/apprunner"
}

module "rds" {
    source = "../../modules/rds"
}

module "file-storage" {
    source = "../../modules/file-storage"
}

module "logs" {
    source = "../../modules/logs"
}
