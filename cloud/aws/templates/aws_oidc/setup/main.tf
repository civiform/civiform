module "setup" {
  source        = "../../../modules/setup"
  app_prefix    = var.app_prefix
  civiform_mode = var.civiform_mode
  aws_region    = var.aws_region
}
