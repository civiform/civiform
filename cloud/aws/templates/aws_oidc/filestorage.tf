resource "aws_s3_bucket" "civiform_files_s3" {
  bucket = var.file_storage_bucket
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
    actions   = ["s3:*"]
    effect    = "Allow"
    resources = ["${aws_s3_bucket.civiform_files_s3.arn}/*"]
    principals {
      type        = "AWS"
      identifiers = [aws_iam_role.apprunner_instance_role.arn]
    }
  }
}

resource "aws_s3_bucket_ownership_controls" "civiform_files_ownership" {
  bucket = aws_s3_bucket.civiform_files_s3.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_logging" "civiform_files_logging" {
  bucket = aws_s3_bucket.civiform_files_s3.id

  target_bucket = var.log_storage_bucket
  target_prefix = "file-access-log/"
}
