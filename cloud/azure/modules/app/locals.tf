locals {
  # The hard-coded zero indexes here are necessary to access the fqdn from the record set associated with it
  # because the private_dns_zone_configs and record_sets blocks expose lists, even if we only have one dns zone
  # and record set configured.
  postgres_private_link = azurerm_private_endpoint.endpoint.private_dns_zone_configs[0].record_sets[0].fqdn
  generated_hostname    = "${var.application_name}-${random_pet.server.id}.azurewebsites.net"
  canary_hostname       = "${var.application_name}-${random_pet.server.id}-canary.azurewebsites.net"

  postgres_password_keyvault_id = "postgres-password"
  app_secret_key_keyvault_id    = "app-secret-key"
  adfs_secret_keyvault_id       = "adfs-secret"
  aws_secret_access_token       = "aws-secret-access-token"
  aws_access_key_id             = "aws-access-key-id"

  app_settings = {
    WEBSITES_ENABLE_APP_SERVICE_STORAGE = false
    PORT                                = 9000

    DOCKER_REGISTRY_SERVER_URL = "https://index.docker.io"

    DB_USERNAME    = "${azurerm_postgresql_server.civiform.administrator_login}@${azurerm_postgresql_server.civiform.name}"
    DB_PASSWORD    = "@Microsoft.KeyVault(SecretUri=${data.azurerm_key_vault_secret.postgres_password.id})"
    DB_JDBC_STRING = "jdbc:postgresql://${local.postgres_private_link}:5432/postgres?ssl=true&sslmode=require"

    STORAGE_SERVICE_NAME = "azure-blob"
    # this allows for the dev instances to get setup
    STAGING_HOSTNAME = (var.staging_hostname != "" ? var.staging_hostname : local.generated_hostname)
    BASE_URL         = "https://${var.custom_hostname != "" ? var.custom_hostname : local.generated_hostname}"

    AZURE_STORAGE_ACCOUNT_NAME      = azurerm_storage_account.files_storage_account.name
    AZURE_STORAGE_ACCOUNT_CONTAINER = azurerm_storage_container.files_container.name

    AWS_SES_SENDER        = var.ses_sender_email
    AWS_ACCESS_KEY_ID     = "@Microsoft.KeyVault(SecretUri=${data.azurerm_key_vault_secret.aws_access_key_id.id})"
    AWS_SECRET_ACCESS_KEY = "@Microsoft.KeyVault(SecretUri=${data.azurerm_key_vault_secret.aws_secret_access_token.id})"
    AWS_REGION            = var.aws_region

    STAGING_ADMIN_LIST     = var.staging_program_admin_notification_mailing_list
    STAGING_TI_LIST        = var.staging_ti_notification_mailing_list
    STAGING_APPLICANT_LIST = var.staging_applicant_notification_mailing_list

    SECRET_KEY = "@Microsoft.KeyVault(SecretUri=${data.azurerm_key_vault_secret.app_secret_key.id})"

    ADFS_CLIENT_ID     = var.adfs_client_id
    ADFS_SECRET        = "@Microsoft.KeyVault(SecretUri=${data.azurerm_key_vault_secret.adfs_secret.id})"
    ADFS_DISCOVERY_URI = var.adfs_discovery_uri

    CIVIFORM_APPLICANT_IDP = var.civiform_applicant_idp

    # The values below are all defaulted to null. If SAML authentication is used, the values can be pulled from the
    # saml_keystore module
    LOGIN_RADIUS_METADATA_URI     = var.login_radius_metadata_uri
    LOGIN_RADIUS_API_KEY          = var.login_radius_api_key
    LOGIN_RADIUS_SAML_APP_NAME    = var.login_radius_saml_app_name
    LOGIN_RADIUS_KEYSTORE_NAME    = (var.saml_keystore_filename != null ? "/saml/${var.saml_keystore_filename}" : "")
    LOGIN_RADIUS_KEYSTORE_PASS    = var.saml_keystore_password
    LOGIN_RADIUS_PRIVATE_KEY_PASS = var.saml_private_key_password

    # In HOCON, env variables set to the empty string are 
    # kept as such (set to empty string, rather than undefined).
    # This allows for the default to include atallclaims and for 
    # azure AD to not include that claim.
    ADFS_ADDITIONAL_SCOPES = ""
  }
}
