data "aws_route53_zone" "civiform" {
  name         = "civiform.dev"
  private_zone = false
}

resource "aws_route53_record" "civiform_domain_record" {
  name    = "staging-aws"
  zone_id = data.aws_route53_zone.civiform.zone_id
  type    = "CNAME"
  records = [aws_apprunner_custom_domain_association.civiform_domain.dns_target]
  ttl     = 60
}

# this updates CNAME records with certificates. 
# The records are only known after AppRunner is up and associated with custom domain.
# to make this work we need to run terraform apply -target aws_apprunner_custom_domain_association.civiform_domain first
resource "aws_route53_record" "civiform_domain_validation" {
  for_each = {
    for dvo in aws_apprunner_custom_domain_association.civiform_domain.certificate_validation_records : dvo.name => {
      name   = dvo.name
      type   = dvo.type
      record = dvo.value
    }
  }

  name    = each.value.name
  zone_id = data.aws_route53_zone.civiform.zone_id
  type    = each.value.type
  records = [each.value.record]
  ttl     = 60
}


resource "aws_apprunner_custom_domain_association" "civiform_domain" {
  domain_name = "staging-aws.civiform.dev"
  service_arn = aws_apprunner_service.civiform_dev.arn
}