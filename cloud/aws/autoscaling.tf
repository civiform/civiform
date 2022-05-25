# TODO: determine reasonable max concurrency for a civiform server
resource "aws_apprunner_auto_scaling_configuration_version" "auto_scaling_config" {
  auto_scaling_configuration_name = "civiform_config"
  max_concurrency                 = 100
  max_size                        = 10
  min_size                        = 1
}
