# TODO this is actually should be an input into filestorage
# b/c it relies on this
resource "aws_s3_bucket" "log_bucket" {
  tags = {
    Name = "${var.app_prefix} Civiform Logs"
    Type = "Civiform Logs"
  }

  bucket = "${var.app_prefix}-logs"
}

resource "aws_s3_bucket_acl" "log_bucket_acl" {
  bucket = aws_s3_bucket.log_bucket.id
  acl    = "log-delivery-write"
}

resource "aws_s3_bucket_versioning" "logging_versioning" {
  bucket = aws_s3_bucket.log_bucket.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "logging_encryption" {
  bucket = aws_s3_bucket.log_bucket.bucket

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = aws_kms_key.backend_storage_key.arn
      sse_algorithm     = "aws:kms"
    }
  }
}
