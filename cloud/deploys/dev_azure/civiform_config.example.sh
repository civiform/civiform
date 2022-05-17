# More details and documentation for these are consolidated in the deploy repo:
# https://github.com/civiform/civiform-deploy/blob/main/civiform_config.example.sh
# Instructions for local dev deploys are available here:
# https://docs.civiform.us/contributor-guide/developer-guide/dev-azure
export CIVIFORM_CLOUD_PROVIDER="azure"
export CIVIFORM_MODE="dev"
export CIVIC_ENTITY_SHORT_NAME="Sooschester"
export CIVIC_ENTITY_FULL_NAME="City of Sooschester"
export CIVIC_ENTITY_SUPPORT_EMAIL_ADDRESS="civiform-azure-staging-email@googlegroups.com"
export CIVIC_ENTITY_LOGO_WITH_NAME_URL="https://www.cityofrochester.gov/images/header-logo.png"
export CIVIC_ENTITY_SMALL_LOGO_URL="https://www.cityofrochester.gov/assets/0/117/8589934986/c6791498-270e-40b1-9b81-aca8a510c1b5.jpg"
export EMAIL_SENDER="ses"
export ADMIN_AUTH_PROVIDER="activeDirectory"
export ADMIN_AUTH_FORMAT="OIDC"
export RESIDENT_AUTH_PROVIDER="loginRadius"
# Choose a unique name. Can only consist of lowercase letters and numbers, and
# must be between 3 and 24 characters long.
export APPLICATION_NAME="CHANGE ME"
export DOCKER_REPOSITORY_NAME="civiform"
# Enter your own docker username if you want to use a local version. Otherwise, enter "civiform".
export DOCKER_USERNAME="CHANGE ME"
export SENDER_EMAIL_ADDRESS="civiform-azure-staging-email@googlegroups.com"
export STAGING_HOSTNAME=""
export STAGING_PROGRAM_ADMIN_NOTIFICATION_MAILING_LIST="civiform-azure-staging-email@googlegroups.com"
export STAGING_TI_NOTIFICATION_MAILING_LIST="civiform-azure-staging-email@googlegroups.com"
export STAGING_APPLICANT_NOTIFICATION_MAILING_LIST="civiform-azure-staging-email@googlegroups.com"
export TERRAFORM_TEMPLATE_DIR="cloud/azure/templates/azure_saml_ses"
export AZURE_LOCATION="eastus"
# Choose a unique name (e.g. "yournamedevlogstorage"). This is the storage
# account name for the deployment log file (not the application server logs).
# Only letters and numbers allowed.
export AZURE_LOG_STORAGE_ACCOUNT_NAME="CHANGE ME"
# The subscription ID can be found in the azure portal.
export AZURE_SUBSCRIPTION="CHANGE ME"
# Use an existing resource group, or specify a new one here to be auto-created.
export AZURE_RESOURCE_GROUP="CHANGE ME"
# Your AWS account username (e.g. my_email@domain.com)
export AWS_USERNAME="CHANGE ME"
# Choose a unique name (e.g. "yournamekeystore"). This is the Azure
# Storage Account name for storing the SAML keystore secrets. Only letters and
# numbers allowed.
export SAML_KEYSTORE_ACCOUNT_NAME="CHANGE ME"
export CIVIFORM_APPLICANT_AUTH_PROTOCOL="saml"
export CUSTOM_HOSTNAME=""
# Choose a unique name.
export KEY_VAULT_NAME="CHANGE ME"
export ADFS_ADMIN_GROUP="0ee46516-6486-48f8-8368-c069c8356ed1"
export LOGIN_RADIUS_API_KEY="1b922870-0719-4970-94e3-cba7b62c5844"
export LOGIN_RADIUS_METADATA_URI="https://civiform-staging.hub.loginradius.com/service/saml/idp/metadata"
# Name from the Login Radius Dashboard that you set up for local development:
# https://docs.civiform.us/contributor-guide/developer-guide/dev-azure#setup-login-radius-for-local-development
export LOGIN_RADIUS_SAML_APP_NAME="CHANGE ME"
