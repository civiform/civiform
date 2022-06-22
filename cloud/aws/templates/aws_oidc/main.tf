module "civiform_app" {
  source = "../../modules/app"

  aws_region = var.aws_region

  civiform_time_zone_id              = var.civiform_time_zone_id
  civic_entity_short_name            = var.civic_entity_short_name
  civic_entity_full_name             = var.civic_entity_full_name
  civic_entity_small_logo_url        = var.civic_entity_small_logo_url
  civic_entity_logo_with_name_url    = var.civic_entity_logo_with_name_url
  civic_entity_support_email_address = var.civic_entity_support_email_address

  ses_sender_email = var.ses_sender_email

  staging_applicant_notification_mailing_list     = var.staging_applicant_notification_mailing_list
  staging_ti_notification_mailing_list            = var.staging_ti_notification_mailing_list
  staging_program_admin_notification_mailing_list = var.staging_program_admin_notification_mailing_list

    civiform_image_repo = var.civiform_image_repo
    civiform_image_tag = var.civiform_image_tag
    app_prefix = var.app_prefix
}

module "email_service" {
  for_each = toset([
    var.ses_sender_email,
    var.staging_applicant_notification_mailing_list,
    var.staging_ti_notification_mailing_list,
    var.staging_program_admin_notification_mailing_list
  ])
  source               = "../../modules/ses"
  sender_email_address = each.key
}