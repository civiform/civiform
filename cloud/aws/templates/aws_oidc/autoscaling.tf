# TODO: determine reasonable max concurrency for a civiform server
resource "aws_apprunner_auto_scaling_configuration_version" "auto_scaling_config" {
  auto_scaling_configuration_name = "civiform_config"
  max_concurrency                 = var.auto_scaling_config["max_concurrency"]
  max_size                        = var.auto_scaling_config["max_size"]
  min_size                        = var.auto_scaling_config["min_size"]
}
