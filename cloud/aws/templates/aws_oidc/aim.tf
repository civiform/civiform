resource "aws_iam_role" "apprunner_instance_role" {
  name               = "${var.app_prefix}-AppRunnerInstanceRole"
  path               = "/"
  assume_role_policy = data.aws_iam_policy_document.apprunner_instance_assume_policy.json
}

data "aws_iam_policy_document" "apprunner_instance_assume_policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["tasks.apprunner.amazonaws.com"]
    }
  }
}

resource "aws_iam_policy" "apprunner_s3_policy" {
  name   = "${var.app_prefix}-apprunner_s3_policy"
  policy = data.aws_iam_policy_document.apprunner_instance_s3_policy.json
}

data "aws_iam_policy_document" "apprunner_instance_s3_policy" {
  statement {
    actions   = ["s3:*"]
    effect    = "Allow"
    resources = ["*"]
  }
}

resource "aws_iam_role_policy_attachment" "apprunner_instance_role_attachment" {
  role       = aws_iam_role.apprunner_instance_role.name
  policy_arn = aws_iam_policy.apprunner_s3_policy.arn
}
