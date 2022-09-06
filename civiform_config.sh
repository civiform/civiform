#! /usr/bin/env bash

# CiviForm deployment configuration file.
#
# Copy this file to civiform_config.sh in the same directory and edit the copy.
#
# cp civiform_config.example.sh civiform_config.sh
#
# Configuration variables must be specified in SCREAMING_SNAKE_CASE with the
# "export" keyword preceding them. All values must be quoted as strings. There
# should be no spaces before or after the equals sign.

#################################################
# Global variables for all CiviForm deployments
#################################################

# Terraform configuration
#################################################

# REQUIRED
# A supported CiviForm cloud provider, lower case.
# "aws" or "azure"
export CIVIFORM_CLOUD_PROVIDER="aws"

# REQUIRED
# One of prod, staging, or dev.
export CIVIFORM_MODE="test"

# REQUIRED
# The template directory for this deployment.
# For aws, use "cloud/aws/templates/aws_oidc"
# For azure, use "cloud/azure/templates/azure_saml_ses"
export TERRAFORM_TEMPLATE_DIR="cloud/aws/templates/aws_oidc"

# REQUIRED
# The docker repository name for retrieving server images.
export DOCKER_REPOSITORY_NAME="civiform"

# REQUIRED
# The docker user name for retrieving server images.
export DOCKER_USERNAME="civiform"

# REQUIRED
# The authentication protocal used for applicant and trusted intermediary accounts.
export CIVIFORM_APPLICANT_AUTH_PROTOCOL="oidc"

# Deployment-specific Civiform configuration
#################################################

# REQUIRED
# The short name for the civic entity. Ex. "Rochester"
export CIVIC_ENTITY_SHORT_NAME="Civiteam"

# REQUIRED
# The full name for the civic entity. Ex. "City of Rochester"
export CIVIC_ENTITY_FULL_NAME="Team of Civiform"

# REQUIRED
# The email address to contact for support with using Civiform. Ex. "Civiform@CityOfRochester.gov
export CIVIC_ENTITY_SUPPORT_EMAIL_ADDRESS="civiform-azure-staging-email@googlegroups.com"

# REQUIRED
# A link to an image of the civic entity logo that includes the entity name, to be used in the header for the "Get Benefits" page
export CIVIC_ENTITY_LOGO_WITH_NAME_URL="https://raw.githubusercontent.com/civiform/staging-aws-deploy/main/logos/civiform-staging-long.png"

# REQUIRED
# A link to an image of the civic entity logo, to be used on the login page
export CIVIC_ENTITY_SMALL_LOGO_URL="https://raw.githubusercontent.com/civiform/staging-aws-deploy/main/logos/civiform-staging.png"

# OPTIONAL
# A link to an 16x16 of 32x32 pixel favicon of the civic entity,
# in format .ico, .png, or .gif.
# Defaults to "https://civiform.us/favicon.png"
export FAVICON_URL=""

# REQUIRED
# The email address to use for the "from" field in emails sent from CiviForm.
export SENDER_EMAIL_ADDRESS="civiform-azure-staging-email@googlegroups.com"
export SES_SENDER_EMAIL="civiform-azure-staging-email@googlegroups.com"

# REQUIRED
# The email address that receives a notifications email each time an applicant
# submits an application to a program in the staging environments, instead of
# sending it to the program administrator's email, as would happen in prod.
export STAGING_PROGRAM_ADMIN_NOTIFICATION_MAILING_LIST="civiform-azure-staging-email@googlegroups.com"

# REQUIRED
# The email address that receives a notifications email each time an applicant
# submits an application to a program in the staging environments, instead of
# sending it to the trusted intermediary's email, as would happen in prod.
export STAGING_TI_NOTIFICATION_MAILING_LIST="civiform-azure-staging-email@googlegroups.com"

# REQUIRED
# The email address that receives a notifications email each time an applicant
# submits an application to a program in the staging environments, instead of
# sending it to the applicant's email, as would happen in prod.
export STAGING_APPLICANT_NOTIFICATION_MAILING_LIST="civiform-azure-staging-email@googlegroups.com"

# REQUIRED
# The domain name for this CiviForm deployment, including the protocol.
# E.g. "https://civiform.seattle.gov"
export BASE_URL="https://staging-aws.civiform.dev"
export CUSTOM_HOSTNAME="staging-aws.civiform.dev"

# OPTIONAL
# The time zone to be used when rendering any times within the CiviForm
# UI. A list of valid time zone identifiers can be found at:
# https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
export CIVIFORM_TIME_ZONE_ID="America/Los_Angeles"

#################################################
# Template variables for: aws_oidc
#################################################

# REQUIRED
# The name to prefix all resources with.
export APP_PREFIX="staging-aws" # max 19 chars, only numbers, letters, dashes, and underscores

export SSL_CERTIFICATE_ARN="arn:aws:acm:us-east-1:664198874744:certificate/1f04bb7f-aab7-444a-8fea-398a3ec34e39"
export FARGATE_DESIRED_TASK_COUNT=1

# REQUIRED
# Which auth provider to use for applicants to login.
# If set to a non-disabled value, you must configure the respective auth parameters
export CIVIFORM_APPLICANT_IDP="generic-oidc"

# generic-oidc Auth configuration
#################################################

# REQUIRED iff CIVIFORM_APPLICANT_IDP="generic-oidc"
# The name to of the OIDC provider.  Must be URL-safe.
# Gets appended to the auth callback URL.
export APPLICANT_OIDC_PROVIDER_NAME="OidcClient"

# REQUIRED iff CIVIFORM_APPLICANT_IDP="generic-oidc"
# The discovery metadata URI provideded by the OIDC provider.
# Usually ends in .well-known/openid-configuration
export APPLICANT_OIDC_DISCOVERY_URI="https://civiform-staging.us.auth0.com/.well-known/openid-configuration"
export ADFS_DISCOVERY_URI="https://civiform-staging.us.auth0.com/.well-known/openid-configuration"

# OPTIONAL
# The type of OIDC flow to execute, and how the data is encoded.
# Defaults to APPLICANT_OIDC_RESPONSE_MODE="form_post" and APPLICANT_OIDC_RESPONSE_TYPE="id_token token"
# See https://auth0.com/docs/authenticate/protocols/oauth#authorization-endpoint
export APPLICANT_OIDC_RESPONSE_MODE="form_post"
export APPLICANT_OIDC_RESPONSE_TYPE="id_token token"

# OPTIONAL
# Any additional claims to request, in addition to the default scopes "openid profile email"
export APPLICANT_OIDC_ADDITIONAL_SCOPES=""

# OPTIONAL
# If your OIDC provider provides the user's language preference,
# provide the profile field it's returned in.
export APPLICANT_OIDC_LOCALE_ATTRIBUTE=""

# OPTIONAL
# The name of the profile field where the user's email is stored.
# Defaults to "email"
export APPLICANT_OIDC_EMAIL_ATTRIBUTE="email"

# OPTIONAL
# The name of the profile field where the user's name is stored.
# If there is only one name field(the display name) use APPLICANT_OIDC_FIRST_NAME_ATTRIBUTE.
# If the name is split into multiple fields, use the APPLICANT_OIDC_MIDDLE_NAME_ATTRIBUTE
# and APPLICANT_OIDC_LAST_NAME_ATTRIBUTE as nessesary.
# Defaults to APPLICANT_OIDC_FIRST_NAME_ATTRIBUTE="name"
export APPLICANT_OIDC_FIRST_NAME_ATTRIBUTE=""
export APPLICANT_OIDC_MIDDLE_NAME_ATTRIBUTE=""
export APPLICANT_OIDC_LAST_NAME_ATTRIBUTE=""

export FEATURE_FLAG_STATUS_TRACKING_ENABLED=true
