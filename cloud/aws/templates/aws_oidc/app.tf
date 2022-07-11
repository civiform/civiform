module "ecs_cluster" {
  source = "cn-terraform/ecs-cluster/aws"
  name   = var.app_prefix
  tags = {
    Name = "${var.app_prefix} Civiform ECS Cluster"
    Type = "Civiform ECS Cluster"
  }
}

# TODO: reconcile with other logs bucket. We should only have one.
module "aws_cw_logs" {
  source    = "cn-terraform/cloudwatch-logs/aws"
  logs_path = "ecslogs/"
  tags = {
    Name = "${var.app_prefix} Civiform Cloud Watch Logs"
    Type = "Civiform Cloud Watch Logs"
  }
}

module "td" {
  source          = "cn-terraform/ecs-fargate-task-definition/aws"
  name_prefix     = var.app_prefix
  container_name  = var.app_prefix
  container_image = "${var.civiform_image_repo}:${var.image_tag}"
  port_mappings = [
    {
      containerPort = var.port
      hostPort      = var.port
      protocol      = "tcp"
    },
    {
      containerPort = 443
      hostPort      = 443
      protocol      = "tcp"
    },
  ]
  map_environment = {
    SECRET_KEY = module.secrets.app_secret_key
    PORT       = var.port

    DB_JDBC_STRING = "jdbc:postgresql://${aws_db_instance.civiform.address}:${aws_db_instance.civiform.port}/postgres?ssl=true&sslmode=require"
    DB_USERNAME    = aws_db_instance.civiform.username
    DB_PASSWORD    = aws_db_instance.civiform.password

    STAGING_HOSTNAME = var.staging_hostname
    BASE_URL         = var.base_url != "" ? var.base_url : "https://${var.custom_hostname}"

    STORAGE_SERVICE_NAME = "s3"
    AWS_S3_BUCKET_NAME   = aws_s3_bucket.civiform_files_s3.id

    CIVIFORM_TIME_ZONE_ID              = var.civiform_time_zone_id
    WHITELABEL_CIVIC_ENTITY_SHORT_NAME = var.civic_entity_short_name
    WHITELABEL_CIVIC_ENTITY_FULL_NAME  = var.civic_entity_full_name
    WHITELABEL_SMALL_LOGO_URL          = var.civic_entity_small_logo_url
    WHITELABEL_LOGO_WITH_NAME_URL      = var.civic_entity_logo_with_name_url
    SUPPORT_EMAIL_ADDRESS              = var.civic_entity_support_email_address

    AWS_SES_SENDER = var.ses_sender_email
    AWS_REGION     = var.aws_region

    STAGING_ADMIN_LIST           = var.staging_program_admin_notification_mailing_list
    STAGING_TI_LIST              = var.staging_ti_notification_mailing_list
    STAGING_APPLICANT_LIST       = var.staging_applicant_notification_mailing_list
    APPLICANT_OIDC_PROVIDER_NAME = var.applicant_oidc_provider_name
    CIVIFORM_APPLICANT_IDP       = var.civiform_applicant_idp
    APPLICANT_OIDC_CLIENT_ID     = var.applicant_oidc_client_id
    APPLICANT_OIDC_CLIENT_SECRET = var.applicant_oidc_client_secret
    APPLICANT_OIDC_DISCOVERY_URI = var.applicant_oidc_discovery_uri
    // TODO Switch to use secrets
    # APPLICANT_OIDC_CLIENT_ID         = module.secrets.applicant_oidc_client_id
    # APPLICANT_OIDC_CLIENT_SECRET     = module.secrets.applicant_oidc_client_secret
    # APPLICANT_OIDC_DISCOVERY_URI     = module.secrets.applicant_oidc_discovery_uri
    APPLICANT_OIDC_RESPONSE_MODE         = var.applicant_oidc_response_mode
    APPLICANT_OIDC_RESPONSE_TYPE         = var.applicant_oidc_response_type
    APPLICANT_OIDC_ADDITIONAL_SCOPES     = var.applicant_oidc_additional_scopes
    APPLICANT_OIDC_LOCALE_ATTRIBUTE      = var.applicant_oidc_locale_attribute
    APPLICANT_OIDC_EMAIL_ATTRIBUTE       = var.applicant_oidc_email_attribute
    APPLICANT_OIDC_FIRST_NAME_ATTRIBUTE  = var.applicant_oidc_first_name_attribute
    APPLICANT_OIDC_MIDDLE_NAME_ATTRIBUTE = var.applicant_oidc_middle_name_attribute
    APPLICANT_OIDC_LAST_NAME_ATTRIBUTE   = var.applicant_oidc_last_name_attribute
  }
  log_configuration = {
    logDriver = "awslogs"
    options = {
      "awslogs-region"        = var.aws_region
      "awslogs-stream-prefix" = "ecs"
      "awslogs-group"         = module.aws_cw_logs.logs_path
      "awslogs-create-group"  = "true",
    }
    secretOptions = null
  }
  tags = {
    Name = "${var.app_prefix} Civiform EC2 Task Definition"
    Type = "Civiform EC2 Task Definition"
  }
}

module "ecs_fargate_service" {
  source      = "cn-terraform/ecs-fargate-service/aws"
  name_prefix = var.app_prefix
  # TODO: use https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/acm_certificate
  default_certificate_arn = "arn:aws:acm:us-east-1:664198874744:certificate/2b765469-2ddd-4b03-94b4-fc670e80f84b"
  ssl_policy              = "ELBSecurityPolicy-FS-1-2-Res-2020-10"
  vpc_id                  = module.vpc.vpc_id
  task_definition_arn     = module.td.aws_ecs_task_definition_td_arn
  container_name          = var.app_prefix
  ecs_cluster_name        = module.ecs_cluster.aws_ecs_cluster_cluster_name
  ecs_cluster_arn         = module.ecs_cluster.aws_ecs_cluster_cluster_arn
  private_subnets         = module.vpc.private_subnets
  public_subnets          = module.vpc.public_subnets

  #TODO: add more listeners. 
  # https://github.com/seattle-uat/civiform-deploy/blob/7ba15ce698de5da6f34d6d9cabec7d451aee9e1c/infra/load_balancer.yaml#L57
  lb_http_ports = { default_http = {
    listener_port     = 9000
    target_group_port = 9000
  } }
  health_check_grace_period_seconds = 20
  tags = {
    Name = "${var.app_prefix} Civiform Fargate Service"
    Type = "Civiform Fargate Service"
  }
}
