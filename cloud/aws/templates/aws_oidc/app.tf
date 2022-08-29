module "ecs_cluster" {
  source  = "cn-terraform/ecs-cluster/aws"
  version = "1.0.10"
  name    = "${var.app_prefix}-civiform"
  tags = {
    Name = "${var.app_prefix} Civiform ECS Cluster"
    Type = "Civiform ECS Cluster"
  }
}

# TODO: reconcile with other logs bucket. We should only have one.
module "aws_cw_logs" {
  source    = "cn-terraform/cloudwatch-logs/aws"
  version   = "1.0.12"
  logs_path = "${var.app_prefix}-civiformlogs/"
  tags = {
    Name = "${var.app_prefix} Civiform Cloud Watch Logs"
    Type = "Civiform Cloud Watch Logs"
  }
}

module "td" {
  source          = "cn-terraform/ecs-fargate-task-definition/aws"
  version         = "1.0.30"
  name_prefix     = "${var.app_prefix}-civiform"
  container_name  = "${var.app_prefix}-civiform"
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

  ecs_task_execution_role_custom_policies = [
    jsonencode(
      {
        "Version" : "2012-10-17",
        "Statement" : [
          {
            "Effect" : "Allow",
            "Action" : [
              "secretsmanager:GetSecretValue"
            ],
            "Resource" : [
              aws_secretsmanager_secret.postgres_username_secret.arn,
              aws_secretsmanager_secret.postgres_password_secret.arn,
              aws_secretsmanager_secret.app_secret_key_secret.arn,
              aws_secretsmanager_secret.api_secret_salt_secret.arn,
              aws_secretsmanager_secret.adfs_secret_secret.arn,
              aws_secretsmanager_secret.adfs_client_id_secret.arn,
              aws_secretsmanager_secret.applicant_oidc_client_secret_secret.arn,
              aws_secretsmanager_secret.applicant_oidc_client_id_secret.arn,
            ]
          },
          {
            "Effect" : "Allow",
            "Action" : [
              "kms:Encrypt",
              "kms:Decrypt",
              "kms:ReEncrypt*",
              "kms:GenerateDataKey*",
              "kms:DescribeKey"
            ],
            "Resource" : [aws_kms_key.civiform_kms_key.arn, aws_kms_key.file_storage_key.arn]
          },
          {
            "Effect" : "Allow",
            "Action" : [
              "s3:*"
            ],
            "Resource" : [
              aws_s3_bucket.civiform_files_s3.arn,
              "${aws_s3_bucket.civiform_files_s3.arn}/*",
            ]
          },
          {
            "Effect" : "Allow",
            "Action" : [
              "ses:*"
            ],
            "Resource" : [
              for email in module.email_service : email.email_arn
            ]
          },
        ]
      }
    )
  ]

  secrets = [
    {
      name      = "DB_USERNAME"
      valueFrom = aws_secretsmanager_secret_version.postgres_username_secret_version.arn
    },
    {
      name      = "DB_PASSWORD"
      valueFrom = aws_secretsmanager_secret_version.postgres_password_secret_version.arn
    },
    {
      name      = "SECRET_KEY"
      valueFrom = aws_secretsmanager_secret_version.app_secret_key_secret_version.arn
    },
    {
      name      = "CIVIFORM_API_SECRET_SALT"
      valueFrom = aws_secretsmanager_secret_version.api_secret_salt_secret_version.arn
    },
    {
      name      = "ADFS_SECRET"
      valueFrom = aws_secretsmanager_secret_version.adfs_secret_secret_version.arn
    },
    {
      name      = "ADFS_CLIENT_ID"
      valueFrom = aws_secretsmanager_secret_version.applicant_oidc_client_id_secret_version.arn
    },
    {
      name      = "APPLICANT_OIDC_CLIENT_ID"
      valueFrom = aws_secretsmanager_secret_version.applicant_oidc_client_id_secret_version.arn
    },
    {
      name      = "APPLICANT_OIDC_CLIENT_SECRET"
      valueFrom = aws_secretsmanager_secret_version.applicant_oidc_client_secret_secret_version.arn
    }
  ]

  map_environment = {
    PORT = var.port

    DB_JDBC_STRING = "jdbc:postgresql://${aws_db_instance.civiform.address}:${aws_db_instance.civiform.port}/postgres?ssl=true&sslmode=require"

    STAGING_HOSTNAME = var.staging_hostname
    BASE_URL         = var.base_url != "" ? var.base_url : "https://${var.custom_hostname}"

    STORAGE_SERVICE_NAME = "s3"
    AWS_S3_BUCKET_NAME   = aws_s3_bucket.civiform_files_s3.id

    CIVIFORM_TIME_ZONE_ID              = var.civiform_time_zone_id
    WHITELABEL_CIVIC_ENTITY_SHORT_NAME = var.civic_entity_short_name
    WHITELABEL_CIVIC_ENTITY_FULL_NAME  = var.civic_entity_full_name
    WHITELABEL_SMALL_LOGO_URL          = var.civic_entity_small_logo_url
    WHITELABEL_LOGO_WITH_NAME_URL      = var.civic_entity_logo_with_name_url
    FAVICON_URL                        = var.favicon_url
    SUPPORT_EMAIL_ADDRESS              = var.civic_entity_support_email_address

    AWS_SES_SENDER = var.sender_email_address
    AWS_REGION     = var.aws_region

    STAGING_ADMIN_LIST                   = var.staging_program_admin_notification_mailing_list
    STAGING_TI_LIST                      = var.staging_ti_notification_mailing_list
    STAGING_APPLICANT_LIST               = var.staging_applicant_notification_mailing_list
    APPLICANT_OIDC_PROVIDER_NAME         = var.applicant_oidc_provider_name
    CIVIFORM_APPLICANT_IDP               = var.civiform_applicant_idp
    APPLICANT_OIDC_RESPONSE_MODE         = var.applicant_oidc_response_mode
    APPLICANT_OIDC_RESPONSE_TYPE         = var.applicant_oidc_response_type
    APPLICANT_OIDC_ADDITIONAL_SCOPES     = var.applicant_oidc_additional_scopes
    APPLICANT_OIDC_LOCALE_ATTRIBUTE      = var.applicant_oidc_locale_attribute
    APPLICANT_OIDC_EMAIL_ATTRIBUTE       = var.applicant_oidc_email_attribute
    APPLICANT_OIDC_FIRST_NAME_ATTRIBUTE  = var.applicant_oidc_first_name_attribute
    APPLICANT_OIDC_MIDDLE_NAME_ATTRIBUTE = var.applicant_oidc_middle_name_attribute
    APPLICANT_OIDC_LAST_NAME_ATTRIBUTE   = var.applicant_oidc_last_name_attribute
    APPLICANT_OIDC_DISCOVERY_URI         = var.applicant_oidc_discovery_uri
    ADFS_DISCOVERY_URI                   = var.adfs_discovery_uri

    CIVIFORM_APPLICATION_STATUS_TRACKING_ENABLED = var.feature_flag_status_tracking_enabled
    CIVIFORM_API_KEYS_BAN_GLOBAL_SUBNET          = var.civiform_api_keys_ban_global_subnet
    CIVIFORM_SERVER_METRICS_ENABLED              = var.civiform_server_metrics_enabled
  }
  log_configuration = {
    logDriver = "awslogs"
    options = {
      "awslogs-region"        = var.aws_region
      "awslogs-stream-prefix" = "ecs"
      "awslogs-group"         = module.aws_cw_logs.logs_path
      "awslogs-create-group"  = "true"
      # Use https://docs.docker.com/config/containers/logging/awslogs/#awslogs-multiline-pattern
      # Logs are streamed via container's stdout. Each line is considered a
      # separate log messsage. To collect stacktraces, which take multiple line,
      # to a single event we consider all lines which start with a whitespace character to be
      # part of the previous line and not a standalone event.
      "awslogs-multiline-pattern" = "^[^\\s]"
    }
    secretOptions = null
  }
  tags = {
    Name = "${var.app_prefix} Civiform EC2 Task Definition"
    Type = "Civiform EC2 Task Definition"
  }
}

module "ecs_fargate_service" {
  source                  = "cn-terraform/ecs-fargate-service/aws"
  version                 = "2.0.34"
  name_prefix             = "${var.app_prefix}-civiform"
  desired_count           = var.fargate_desired_task_count
  default_certificate_arn = var.ssl_certificate_arn
  ssl_policy              = "ELBSecurityPolicy-FS-1-2-Res-2020-10"
  vpc_id                  = module.vpc.vpc_id
  task_definition_arn     = module.td.aws_ecs_task_definition_td_arn
  container_name          = "${var.app_prefix}-civiform"
  ecs_cluster_name        = module.ecs_cluster.aws_ecs_cluster_cluster_name
  ecs_cluster_arn         = module.ecs_cluster.aws_ecs_cluster_cluster_arn
  private_subnets         = module.vpc.private_subnets
  public_subnets          = module.vpc.public_subnets

  lb_http_ports = {
    default_http = {
      type          = "redirect"
      listener_port = 80
      port          = 443
      protocol      = "HTTPS"
      host          = "#{host}"
      path          = "/#{path}"
      query         = "#{query}"
      status_code   = "HTTP_301"
    }
  }
  lb_https_ports = {
    default_http = {
      listener_port         = 443
      target_group_port     = var.port
      target_group_protocol = "HTTP"
    }
  }
  health_check_grace_period_seconds                = 20
  lb_target_group_health_check_path                = "/playIndex"
  lb_target_group_health_check_interval            = 10
  lb_target_group_health_check_timeout             = 5
  lb_target_group_health_check_healthy_threshold   = 2
  lb_target_group_health_check_unhealthy_threshold = 2

  tags = {
    Name = "${var.app_prefix} Civiform Fargate Service"
    Type = "Civiform Fargate Service"
  }
}
