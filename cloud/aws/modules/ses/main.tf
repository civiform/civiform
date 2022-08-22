resource "aws_ses_email_identity" "email" {
  email = var.sender_email_address
}

output "email_arn" {
  value = aws_ses_email_identity.email.arn
}
