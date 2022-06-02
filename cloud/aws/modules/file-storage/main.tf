resource "aws_s3_bucket" "civiform_files_s3" {
  bucket = "civiform-files-s3"
}

resource "aws_s3_bucket_public_access_block" "civiform_files_access" {
  bucket                  = aws_s3_bucket.civiform_files_s3.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_policy" "civiform_files_policy" {
  bucket = aws_s3_bucket.civiform_files_s3.id
  policy = data.aws_iam_policy_document.civiform_files_policy.json
}

data "aws_iam_policy_document" "civiform_files_policy" {
  statement {
    actions   = ["s3:*"]
    effect    = "Deny"
    resources = ["${aws_s3_bucket.civiform_files_s3.arn}/*"]
    not_principals {
      type        = "AWS"
      identifiers = [aws_iam_role.apprunner_instance_role.arn]
    }
  }
  statement {
    actions   = ["s3:GetObject", "s3:PutObject"]
    effect    = "Allow"
    resources = ["${aws_s3_bucket.civiform_files_s3.arn}/*"]
    principals {
      type        = "AWS"
      identifiers = [aws_iam_role.apprunner_instance_role.arn]
    }
  }
}