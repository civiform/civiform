resource "aws_s3_bucket" "backend_state_bucket" {
  bucket = "${var.app_prefix}-backendstate"
}

resource "aws_s3_bucket_versioning" "backend_state_versioning" {
  bucket = aws_s3_bucket.backend_state_bucket.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_kms_key" "backend_storage_key" {
  description             = "This key is used to encrypt backend state bucket objects"
  deletion_window_in_days = 10
}

resource "aws_s3_bucket_server_side_encryption_configuration" "backend_state_encryption" {
  bucket = aws_s3_bucket.backend_state_bucket.bucket

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = aws_kms_key.backend_storage_key.arn
      sse_algorithm     = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_policy" "backend_state_bucket_policy" {
  bucket = aws_s3_bucket.backend_state_bucket.id
  policy = data.aws_iam_policy_document.backend_state_files_policy.json
}

data "aws_caller_identity" "current" {}

data "aws_iam_policy_document" "backend_state_files_policy" {
  statement {
    actions   = ["s3:ListBucket"]
    effect    = "Allow"
    resources = [aws_s3_bucket.backend_state_bucket.arn]
    principals {
      type        = "AWS"
      identifiers = [data.aws_caller_identity.current.arn]
    }
  }
  statement {
    actions   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
    effect    = "Allow"
    resources = ["${aws_s3_bucket.backend_state_bucket.arn}/*"]
    principals {
      type        = "AWS"
      identifiers = [data.aws_caller_identity.current.arn]
    }
  }
}

resource "aws_dynamodb_table" "state_locking" {
  hash_key = "LockID"
  name     = "${var.app_prefix}-locktable"
  attribute {
    name = "LockID"
    type = "S"
  }
  billing_mode = "PAY_PER_REQUEST"
}

