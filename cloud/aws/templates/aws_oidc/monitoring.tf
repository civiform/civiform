resource "aws_prometheus_workspace" "metrics" {
  alias = "${var.app_prefix}-CiviForm_metrics"
}

resource "aws_grafana_workspace" "CiviForm_metrics" {
  name                     = "${var.app_prefix}-civiform-metrics"
  description              = "Grafana instance for ${var.app_prefix}-civiform-metrics"
  data_sources             = ["PROMETHEUS"]
  account_access_type      = "CURRENT_ACCOUNT"
  role_arn                 = aws_iam_role.grafana_assume_role.arn
  authentication_providers = ["AWS_SSO"]
  permission_type          = "SERVICE_MANAGED"
}

resource "aws_iam_role" "grafana_assume_role" {
  name = "${var.app_prefix}-grafana-assume-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = "grafana.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_policy" "civiform_monitoring_role_policy" {
  name = "${var.app_prefix}-civiform-monitoring-role-policy"
  policy = jsonencode(
    {
      "Version" : "2012-10-17",
      "Statement" : [
        {
          Effect = "Allow",
          Action = [
            "tag:GetTagValues",
            "tag:GetTagKeys"
          ],
          Resource = "*"
        },
        {
          Effect = "Allow",
          Action = [
            "logs:DescribeLogGroups"
          ],
          Resource = "*"
        },
        {
          Effect = "Allow",
          Action = [
            "aps:CreateWorkspace",
            "aps:DescribeWorkspace",
            "aps:UpdateWorkspaceAlias",
            "aps:DeleteWorkspace",
            "aps:ListWorkspaces",
            "aps:DescribeAlertManagerDefinition",
            "aps:DescribeRuleGroupsNamespace",
            "aps:CreateAlertManagerDefinition",
            "aps:CreateRuleGroupsNamespace",
            "aps:DeleteAlertManagerDefinition",
            "aps:DeleteRuleGroupsNamespace",
            "aps:ListRuleGroupsNamespaces",
            "aps:PutAlertManagerDefinition",
            "aps:PutRuleGroupsNamespace",
            "aps:TagResource",
            "aps:UntagResource",
            "aps:CreateLoggingConfiguration",
            "aps:UpdateLoggingConfiguration",
            "aps:DeleteLoggingConfiguration",
            "aps:DescribeLoggingConfiguration"
          ],
          Resource = "*"
        }
      ]
    }
  )
}

resource "aws_iam_role_policy_attachment" "civiform_monitoring_role_policies_attach" {
  role       = aws_iam_role.grafana_assume_role.name
  policy_arn = aws_iam_policy.civiform_monitoring_role_policy.arn
}
