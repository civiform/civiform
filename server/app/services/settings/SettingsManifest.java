package services.settings;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import javax.inject.Inject;

public final class SettingsManifest extends AbstractSettingsManifest {

  private final ImmutableMap<String, SettingsSection> settingsSections;

  @Inject
  public SettingsManifest() {
    this.settingsSections = GENERATED_SECTIONS;
  }

  @VisibleForTesting
  public SettingsManifest(ImmutableMap<String, SettingsSection> settingsSections) {
    this.settingsSections = checkNotNull(settingsSections);
  }

  @Override
  public ImmutableMap<String, SettingsSection> getSections() {
    return settingsSections;
  }

  private static final ImmutableMap<String, SettingsSection> GENERATED_SECTIONS =
      ImmutableMap.of(
          "Branding",
          SettingsSection.create(
              "Branding",
              "Configuration options for CiviForm branding.",
              ImmutableList.of(),
              ImmutableList.of(
                  SettingDescription.create(
                      "WHITELABEL_SMALL_LOGO_URL",
                      "Small logo for the civic entity used on the login page.",
                      SettingType.STRING),
                  SettingDescription.create(
                      "WHITELABEL_LOGO_WITH_NAME_URL",
                      "Logo with civic entity name used on the applicant-facing program index"
                          + " page.",
                      SettingType.STRING),
                  SettingDescription.create(
                      "WHITELABEL_CIVIC_ENTITY_SHORT_NAME",
                      "The short display name of the civic entity, will use 'TestCity' if not set.",
                      SettingType.STRING),
                  SettingDescription.create(
                      "WHITELABEL_CIVIC_ENTITY_FULL_NAME",
                      "The full display name of the civic entity, will use 'City of TestCity' if"
                          + " not set.",
                      SettingType.STRING),
                  SettingDescription.create(
                      "FAVICON_URL",
                      "The URL of a 32x32 or 16x16 pixel"
                          + " [favicon](https://developer.mozilla.org/en-US/docs/Glossary/Favicon)"
                          + " image, in GIF, PNG, or ICO format.",
                      SettingType.STRING))),
          "External service dependencies",
          SettingsSection.create(
              "External service dependencies",
              "Configures connections to external services the CiviForm server relies on.",
              ImmutableList.of(
                  SettingsSection.create(
                      "Applicant Identity Provider",
                      "Configuration options for the [applicant identity"
                          + " provider](https://docs.civiform.us/contributor-guide/developer-guide/authentication-providers#applicant-authentication).",
                      ImmutableList.of(
                          SettingsSection.create(
                              "Oracle Identity Cloud Service",
                              "Configuration options for the"
                                  + " [idcs](https://docs.civiform.us/contributor-guide/developer-guide/authentication-providers#oracle-idcs)"
                                  + " provider.",
                              ImmutableList.of(),
                              ImmutableList.of(
                                  SettingDescription.create(
                                      "IDCS_CLIENT_ID",
                                      "An opaque public identifier for apps that use OIDC (OpenID"
                                          + " Connect) to request data from authorization servers,"
                                          + " specifically communicating with IDCS. A Civiform"
                                          + " instance is always the client.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "IDCS_SECRET",
                                      "A secret known only to the client (Civiform) and"
                                          + " authorization server, specifically for IDCS OIDC"
                                          + " systems. This secret essentially acts as the"
                                          + " client’s “password” for accessing data from the auth"
                                          + " server.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "IDCS_DISCOVERY_URI",
                                      "A URL that returns a JSON listing of OIDC (OpenID Connect)"
                                          + " data associated with the IDCS auth provider.",
                                      SettingType.STRING))),
                          SettingsSection.create(
                              "Login Radius",
                              "Configuration options for the"
                                  + " [login-radius](https://docs.civiform.us/contributor-guide/developer-guide/authentication-providers#loginradius-saml)"
                                  + " provider",
                              ImmutableList.of(),
                              ImmutableList.of(
                                  SettingDescription.create(
                                      "LOGIN_RADIUS_API_KEY",
                                      "The API key used to interact with LoginRadius.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "LOGIN_RADIUS_METADATA_URI",
                                      "The base URL to construct SAML endpoints, based on the"
                                          + " SAML2 spec.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "LOGIN_RADIUS_SAML_APP_NAME",
                                      "The name for the app, based on the SAML2 spec.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "LOGIN_RADIUS_KEYSTORE_NAME",
                                      "Name of the SAML2 keystore, used to store digital"
                                          + " certificates and private keys for SAML auth.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "LOGIN_RADIUS_KEYSTORE_PASS",
                                      "The password used the protect the integrity of the SAML"
                                          + " keystore file.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "LOGIN_RADIUS_PRIVATE_KEY_PASS",
                                      "The password used to protect the private key of the SAML"
                                          + " digital certificate.",
                                      SettingType.STRING))),
                          SettingsSection.create(
                              "OpenID Connect",
                              "Configuration options for the"
                                  + " [generic-oidc](https://docs.civiform.us/contributor-guide/developer-guide/authentication-providers#generic-oidc-oidc)"
                                  + " provider.",
                              ImmutableList.of(),
                              ImmutableList.of(
                                  SettingDescription.create(
                                      "APPLICANT_OIDC_PROVIDER_LOGOUT",
                                      "Enables [central"
                                          + " logout](https://docs.civiform.us/contributor-guide/developer-guide/authentication-providers#logout).",
                                      SettingType.BOOLEAN),
                                  SettingDescription.create(
                                      "APPLICANT_OIDC_OVERRIDE_LOGOUT_URL",
                                      "By default the 'end_session_endpoint' from the auth"
                                          + " provider discovery metadata file is used as the"
                                          + " logout endpoint. However for some integrations that"
                                          + " standard flow might not work and we need to override"
                                          + " logout URL.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "APPLICANT_OIDC_POST_LOGOUT_REDIRECT_PARAM",
                                      "URL param used to pass the post logout redirect url in the"
                                          + " logout request to the auth provider. It defaults to"
                                          + " 'post_logout_redirect_uri' if this variable is"
                                          + " unset. If this variable is set to the empty string,"
                                          + " the post logout redirect url is not passed at all"
                                          + " and instead it needs to be hardcoded on the the auth"
                                          + " provider (otherwise the user won't be redirected"
                                          + " back to civiform after logout).",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "APPLICANT_OIDC_PROVIDER_NAME",
                                      "The name of the OIDC (OpenID Connect) auth provider"
                                          + " (server), such as “Auth0” or “LoginRadius”.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "APPLICANT_OIDC_CLIENT_ID",
                                      "An opaque public identifier for apps that use OIDC (OpenID"
                                          + " Connect) to request data from authorization servers."
                                          + " A Civiform instance is always the client.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "APPLICANT_OIDC_CLIENT_SECRET",
                                      "A secret known only to the client (Civiform) and"
                                          + " authorization server. This secret essentially acts"
                                          + " as the client’s “password” for accessing data from"
                                          + " the auth server.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "APPLICANT_OIDC_DISCOVERY_URI",
                                      "A URL that returns a JSON listing of OIDC (OpenID Connect)"
                                          + " data associated with a given auth provider.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "APPLICANT_OIDC_RESPONSE_MODE",
                                      "Informs the auth server of the desired auth processing"
                                          + " flow, based on the OpenID Connect spec.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "APPLICANT_OIDC_RESPONSE_TYPE",
                                      "Informs the auth server of the mechanism to be used for"
                                          + " returning response params from the auth endpoint,"
                                          + " based on the OpenID Connect spec.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "APPLICANT_OIDC_ADDITIONAL_SCOPES",
                                      "Scopes the client (CiviForm) is requesting in addition to"
                                          + " the standard scopes the OpenID Connect spec"
                                          + " provides.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "APPLICANT_OIDC_LOCALE_ATTRIBUTE",
                                      "The locale of the user, such as “en-US”.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "APPLICANT_OIDC_EMAIL_ATTRIBUTE",
                                      "The OIDC attribute name for the user’s email address.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "APPLICANT_OIDC_FIRST_NAME_ATTRIBUTE",
                                      "The OIDC attribute name for the user’s first name.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "APPLICANT_OIDC_MIDDLE_NAME_ATTRIBUTE",
                                      "The OIDC attribute name for the user’s middle name.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "APPLICANT_OIDC_LAST_NAME_ATTRIBUTE",
                                      "The OIDC attribute name for the user’s last name.",
                                      SettingType.STRING))),
                          SettingsSection.create(
                              "Login.gov",
                              "Configuration options for the"
                                  + " [login-gov](https://docs.civiform.us/contributor-guide/developer-guide/authentication-providers#login.gov-oidc)"
                                  + " provider",
                              ImmutableList.of(),
                              ImmutableList.of(
                                  SettingDescription.create(
                                      "LOGIN_GOV_CLIENT_ID",
                                      "An opaque public identifier for apps that use OIDC (OpenID"
                                          + " Connect) to request data from authorization servers,"
                                          + " specifically communicating with Login.gov. A"
                                          + " Civiform instance is always the client.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "LOGIN_GOV_DISCOVERY_URI",
                                      "A URL that returns a JSON listing of OIDC (OpenID Connect)"
                                          + " data associated with a given auth provider,"
                                          + " specifically for Login.gov.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "LOGIN_GOV_ADDITIONAL_SCOPES",
                                      "Scopes the client (CiviForm) is requesting in addition to"
                                          + " the standard scopes the OpenID Connect spec"
                                          + " provides. Scopes should be separated by a space.",
                                      SettingType.STRING),
                                  SettingDescription.create(
                                      "LOGIN_GOV_ACR_VALUE",
                                      "[Authentication Context Class Reference"
                                          + " requests](https://developers.login.gov/oidc/#request-parameters)."
                                          + " ial/1 is for open registration, email only. ial/2 is"
                                          + " for requiring identity verification.",
                                      SettingType.ENUM)))),
                      ImmutableList.of(
                          SettingDescription.create(
                              "CIVIFORM_APPLICANT_IDP",
                              "What identity provider to use for applicants.",
                              SettingType.ENUM),
                          SettingDescription.create(
                              "APPLICANT_REGISTER_URI",
                              "URI to create a new account in the applicant identity provider.",
                              SettingType.STRING),
                          SettingDescription.create(
                              "APPLICANT_PORTAL_NAME",
                              "The name of the portal that applicants log into, used in sentences"
                                  + " like 'Log into your APPLICANT_PORTAL_NAME account.'",
                              SettingType.STRING))),
                  SettingsSection.create(
                      "Administrator Identity Provider",
                      "Configuration options for the [administrator identity"
                          + " provider](https://docs.civiform.us/contributor-guide/developer-guide/authentication-providers#admin-authentication).",
                      ImmutableList.of(),
                      ImmutableList.of(
                          SettingDescription.create(
                              "ADFS_CLIENT_ID",
                              "An opaque public identifier for apps that use OIDC (OpenID Connect)"
                                  + " to request data from authorization servers, specifically"
                                  + " communicating with ADFS. A Civiform instance is always the"
                                  + " client.",
                              SettingType.STRING),
                          SettingDescription.create(
                              "ADFS_SECRET",
                              "A secret known only to the client (Civiform) and authorization"
                                  + " server. This secret essentially acts as the client’s"
                                  + " “password” for accessing data from the auth server.",
                              SettingType.STRING),
                          SettingDescription.create(
                              "ADFS_DISCOVERY_URI",
                              "A URL that returns a JSON listing of OIDC (OpenID Connect) data"
                                  + " associated with the IDCS auth provider.",
                              SettingType.STRING),
                          SettingDescription.create(
                              "ADFS_GLOBAL_ADMIN_GROUP",
                              "The name of the admin group in Active Directory, typically used to"
                                  + " tell if a user is a global admin.",
                              SettingType.STRING),
                          SettingDescription.create(
                              "ADFS_ADDITIONAL_SCOPES",
                              "Scopes the client (CiviForm) is requesting in addition to the"
                                  + " standard scopes the OpenID Connect spec provides. Scopes"
                                  + " should be separated by a space.",
                              SettingType.STRING),
                          SettingDescription.create(
                              "AD_GROUPS_ATTRIBUTE_NAME",
                              "The attribute name for looking up the groups associated with a"
                                  + " particular user.",
                              SettingType.STRING))),
                  SettingsSection.create(
                      "Database",
                      "Configures the connection to the PostgreSQL database.",
                      ImmutableList.of(),
                      ImmutableList.of(
                          SettingDescription.create(
                              "DATABASE_APPLY_DESTRUCTIVE_CHANGES",
                              "If enabled, [playframework down"
                                  + " evolutions](https://www.playframework.com/documentation/2.8.x/Evolutions#Evolutions-scripts)"
                                  + " are automatically applied on server start if needed.",
                              SettingType.BOOLEAN),
                          SettingDescription.create(
                              "DATABASE_CONNECTION_POOL_SIZE",
                              "Sets how many connections to the database are maintained.",
                              SettingType.INT),
                          SettingDescription.create(
                              "DB_JDBC_STRING", "The database URL.", SettingType.STRING),
                          SettingDescription.create(
                              "DB_USERNAME",
                              "The username used to connect to the database.",
                              SettingType.STRING),
                          SettingDescription.create(
                              "DB_PASSWORD",
                              "The password used to connect to the database.",
                              SettingType.STRING))),
                  SettingsSection.create(
                      "Application File Upload Storage",
                      "Configuration options for the application file upload storage provider",
                      ImmutableList.of(),
                      ImmutableList.of(
                          SettingDescription.create(
                              "STORAGE_SERVICE_NAME",
                              "What static file storage provider to use.",
                              SettingType.ENUM),
                          SettingDescription.create(
                              "AWS_S3_BUCKET_NAME",
                              "s3 bucket to store files in.",
                              SettingType.STRING),
                          SettingDescription.create(
                              "AWS_S3_FILE_LIMIT_MB",
                              "The max size (in Mb) of files uploaded to s3.",
                              SettingType.STRING),
                          SettingDescription.create(
                              "AZURE_STORAGE_ACCOUNT_NAME",
                              "The azure account name where the blob storage service exists.",
                              SettingType.STRING),
                          SettingDescription.create(
                              "AZURE_STORAGE_ACCOUNT_CONTAINER",
                              "Azure blob storage container name to store files in.",
                              SettingType.STRING),
                          SettingDescription.create(
                              "AZURE_LOCAL_CONNECTION_STRING",
                              "Allows local [Azurite"
                                  + " emulator](https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite)"
                                  + " to be used for developer deployments.",
                              SettingType.STRING))),
                  SettingsSection.create(
                      "ESRI Address Validation",
                      "Configuration options for the ESRI GIS client and address"
                          + " validation/correction feature.",
                      ImmutableList.of(),
                      ImmutableList.of(
                          SettingDescription.create(
                              "ESRI_ADDRESS_CORRECTION_ENABLED",
                              "Enables the feature that allows address correction for address"
                                  + " questions.",
                              SettingType.BOOLEAN),
                          SettingDescription.create(
                              "ESRI_FIND_ADDRESS_CANDIDATES_URL",
                              "The URL CiviForm will use to call Esri’s [findAddressCandidates"
                                  + " service](https://developers.arcgis.com/rest/geocode/api-reference/geocoding-find-address-candidates.htm).",
                              SettingType.STRING),
                          SettingDescription.create(
                              "ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ENABLED",
                              "Enables the feature that allows for service area validation of a"
                                  + " corrected address. ESRI_ADDRESS_CORRECTION_ENABLED needs to"
                                  + " be enabled.",
                              SettingType.BOOLEAN),
                          SettingDescription.create(
                              "ESRI_ADDRESS_SERVICE_AREA_VALIDATION_LABELS",
                              "Human readable labels used to present the service area validation"
                                  + " options in CiviForm’s admin UI.",
                              SettingType.LIST_OF_STRINGS),
                          SettingDescription.create(
                              "ESRI_ADDRESS_SERVICE_AREA_VALIDATION_IDS",
                              "The value CiviForm uses to validate if an address is in a service"
                                  + " area.",
                              SettingType.LIST_OF_STRINGS),
                          SettingDescription.create(
                              "ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS",
                              "The URL CiviForm will use to call Esri’s [map query"
                                  + " service](https://developers.arcgis.com/rest/services-reference/enterprise/query-feature-service-layer-.htm)"
                                  + " for service area validation.",
                              SettingType.LIST_OF_STRINGS),
                          SettingDescription.create(
                              "ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ATTRIBUTES",
                              "The attribute CiviForm checks from the service area validation"
                                  + " response to get the service area validation ID.",
                              SettingType.LIST_OF_STRINGS),
                          SettingDescription.create(
                              "ESRI_EXTERNAL_CALL_TRIES",
                              "The number of tries CiviForm will attempt requests to external Esri"
                                  + " services.",
                              SettingType.INT)))),
              ImmutableList.of(
                  SettingDescription.create(
                      "AWS_REGION",
                      "Region where the AWS SES service exists. If STORAGE_SERVICE_NAME is set to"
                          + " 'aws', it is also the region where the AWS s3 service exists.",
                      SettingType.STRING),
                  SettingDescription.create(
                      "AWS_SES_SENDER",
                      "The email address used for the 'from' email header for emails sent by"
                          + " CiviForm.",
                      SettingType.STRING))),
          "Email addresses",
          SettingsSection.create(
              "Email addresses",
              "Configuration options for [CiviForm email"
                  + " usage](https://docs.civiform.us/it-manual/sre-playbook/email-configuration).",
              ImmutableList.of(),
              ImmutableList.of(
                  SettingDescription.create(
                      "SUPPORT_EMAIL_ADDRESS",
                      "This email address is listed in the footer for applicants to contact"
                          + " support.",
                      SettingType.STRING),
                  SettingDescription.create(
                      "IT_EMAIL_ADDRESS",
                      "This email address receives error notifications from CiviForm when things"
                          + " break.",
                      SettingType.STRING),
                  SettingDescription.create(
                      "STAGING_ADMIN_LIST",
                      "If this is a staging deployment, the application notification email is sent"
                          + " to this email address instead of the program administrator's email"
                          + " address.",
                      SettingType.STRING),
                  SettingDescription.create(
                      "STAGING_TI_LIST",
                      "If this is a staging deployment, the application notification email is sent"
                          + " to this email address instead of the trusted intermediary's email"
                          + " address.",
                      SettingType.STRING),
                  SettingDescription.create(
                      "STAGING_APPLICANT_LIST",
                      "If this is a staging deployment, the application notification email is sent"
                          + " to this email address instead of the applicant's email address.",
                      SettingType.STRING))),
          "Custom Text",
          SettingsSection.create(
              "Custom Text",
              "Text specific to a civic entity.",
              ImmutableList.of(),
              ImmutableList.of(
                  SettingDescription.create(
                      "COMMON_INTAKE_MORE_RESOURCES_LINK_TEXT",
                      "The text for a link on the Common Intake confirmation page that links to"
                          + " more resources. Shown when the applicant is not eligible for any"
                          + " programs in CiviForm.",
                      SettingType.STRING),
                  SettingDescription.create(
                      "COMMON_INTAKE_MORE_RESOURCES_LINK_HREF",
                      "The HREF for a link on the Common Intake confirmation page that links to"
                          + " more resources. Shown when the applicant is not eligible for any"
                          + " programs in CiviForm.",
                      SettingType.STRING))),
          "Observability",
          SettingsSection.create(
              "Observability",
              "Configuration options for CiviForm observability features.",
              ImmutableList.of(),
              ImmutableList.of(
                  SettingDescription.create(
                      "CIVIFORM_SERVER_METRICS_ENABLED",
                      "If enabled, allows server Prometheus metrics to be retrieved via the"
                          + " '/metrics' URL path.  If disabled, '/metrics' returns a 404.",
                      SettingType.BOOLEAN),
                  SettingDescription.create(
                      "MEASUREMENT_ID",
                      "The Google Analytics tracking ID.  If set, Google Analytics JavaScript"
                          + " scripts are added to the CiviForm pages.",
                      SettingType.STRING))),
          "Data Export API",
          SettingsSection.create(
              "Data Export API",
              "Configuration options for the [CiviForm"
                  + " API](https://docs.civiform.us/it-manual/api).",
              ImmutableList.of(),
              ImmutableList.of(
                  SettingDescription.create(
                      "CIVIFORM_API_SECRET_SALT",
                      "A cryptographic [secret"
                          + " salt](https://en.wikipedia.org/wiki/Salt_(cryptography)) used for"
                          + " salting API keys before storing their hash values in the database."
                          + " This value should be kept strictly secret. If one suspects the"
                          + " secret has been leaked or otherwise comprised it should be changed"
                          + " and all active API keys should be retired and reissued. Default"
                          + " value is 'changeme'.",
                      SettingType.STRING),
                  SettingDescription.create(
                      "CIVIFORM_API_KEYS_BAN_GLOBAL_SUBNET",
                      "When true prevents the CiviForm admin from issuing API keys that allow"
                          + " callers from all IP addresses (i.e. a CIDR mask of /0).",
                      SettingType.BOOLEAN),
                  SettingDescription.create(
                      "CIVIFORM_API_APPLICATIONS_LIST_MAX_PAGE_SIZE",
                      "An integer specifying the maximum number of entries returned in a page of"
                          + " results for the applications export API.",
                      SettingType.INT))),
          "Durable Jobs",
          SettingsSection.create(
              "Durable Jobs",
              "Configuration options for the CiviForm Job Runner.",
              ImmutableList.of(),
              ImmutableList.of(
                  SettingDescription.create(
                      "DURABLE_JOBS_POLL_INTERVAL_SECONDS",
                      "An integer specifying the polling interval in seconds for the durable job"
                          + " system. A smaller number here increases the polling frequency, which"
                          + " results in jobs running sooner when they are scheduled to be run"
                          + " immediately, at the cost of more pressure on the database. Default"
                          + " value is 5.",
                      SettingType.INT),
                  SettingDescription.create(
                      "DURABLE_JOBS_JOB_TIMEOUT_MINUTES",
                      "An integer specifying the timeout in minutes for durable jobs i.e. how long"
                          + " a single job is allowed to run before the system attempts to"
                          + " interrupt it. Default value is 30.",
                      SettingType.INT),
                  SettingDescription.create(
                      "DURABLE_JOBS_THREAD_POOL_SIZE",
                      "The number of server threads available for the durable job runner. More"
                          + " than a single thread will the server execute multiple jobs in"
                          + " parallel. Default value is 1.",
                      SettingType.INT))),
          "Feature Flags",
          SettingsSection.create(
              "Feature Flags",
              "Configuration options to enable or disable optional or in-development features.",
              ImmutableList.of(),
              ImmutableList.of(
                  SettingDescription.create(
                      "CF_OPTIONAL_QUESTIONS",
                      "If enabled, allows questions to be optional in programs. Is enabled by"
                          + " default.",
                      SettingType.BOOLEAN),
                  SettingDescription.create(
                      "ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS",
                      "If enabled, CiviForm Admins are able to see all applications for all"
                          + " programs. Is disabled by default.",
                      SettingType.BOOLEAN),
                  SettingDescription.create(
                      "SHOW_CIVIFORM_IMAGE_TAG_ON_LANDING_PAGE",
                      "If enabled, the value of CIVIFORM_IMAGE_TAG will be shown on the login"
                          + " screen. Is disabled by default.",
                      SettingType.BOOLEAN),
                  SettingDescription.create(
                      "FEATURE_FLAG_OVERRIDES_ENABLED",
                      "Allows feature flags to be overridden via request cookies. Is used by"
                          + " browswer tests. Should only be enabled in test and staging"
                          + " deployments.",
                      SettingType.BOOLEAN),
                  SettingDescription.create(
                      "INTAKE_FORM_ENABLED",
                      "Enables the Common Intake Form feature.",
                      SettingType.BOOLEAN),
                  SettingDescription.create(
                      "NONGATED_ELIGIBILITY_ENABLED",
                      "Enables the feature that allows setting eligibility criteria to non-gating.",
                      SettingType.BOOLEAN),
                  SettingDescription.create(
                      "STAGING_ADD_NOINDEX_META_TAG",
                      "If this is a staging deployment and this variable is set to true, a [robots"
                          + " noindex](https://developers.google.com/search/docs/crawling-indexing/robots-meta-tag)"
                          + " metadata tag is added to the CiviForm pages. This causes the staging"
                          + " site to not be listed on search engines.",
                      SettingType.BOOLEAN),
                  SettingDescription.create(
                      "STAGING_DISABLE_DEMO_MODE_LOGINS",
                      "If this is a staging deployment and this variable is set to true, the 'DEMO"
                          + " MODE. LOGIN AS:' buttons are not shown on the login page.",
                      SettingType.BOOLEAN),
                  SettingDescription.create(
                      "PHONE_QUESTION_TYPE_ENABLED",
                      "Enables the phone number question type.",
                      SettingType.BOOLEAN),
                  SettingDescription.create(
                      "PUBLISH_SINGLE_PROGRAM_ENABLED",
                      "Enables the feature that allows publishing a single program on its own.",
                      SettingType.BOOLEAN))),
          "ROOT",
          SettingsSection.create(
              "ROOT",
              "Top level vars",
              ImmutableList.of(),
              ImmutableList.of(
                  SettingDescription.create(
                      "SECRET_KEY",
                      "The [secret"
                          + " key](http://www.playframework.com/documentation/latest/ApplicationSecret)"
                          + " is used to sign Play's session cookie. This must be changed for"
                          + " production.",
                      SettingType.STRING),
                  SettingDescription.create(
                      "BASE_URL",
                      "The URL of the CiviForm deployment.  Must start with 'https://' or"
                          + " 'http://'.",
                      SettingType.STRING),
                  SettingDescription.create(
                      "STAGING_HOSTNAME",
                      "DNS name of the staging deployment.  Must not start with 'https://' or"
                          + " 'http://'.",
                      SettingType.STRING),
                  SettingDescription.create(
                      "CIVIFORM_SUPPORTED_LANGUAGES",
                      "The languages that applicants can choose from when specifying their"
                          + " language preference and that admins can choose from when adding"
                          + " translations for programs and applications.",
                      SettingType.LIST_OF_STRINGS),
                  SettingDescription.create(
                      "CIVIFORM_TIME_ZONE_ID",
                      "A Java [time zone"
                          + " ID](https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html)"
                          + " indicating the time zone for this CiviForm deployment. All times in"
                          + " the system will be calculated in this zone. Default value is"
                          + " 'America/Los_Angeles'",
                      SettingType.STRING),
                  SettingDescription.create(
                      "CIVIFORM_IMAGE_TAG",
                      "The tag of the docker image this server is running inside. Is added as a"
                          + " HTML meta tag with name 'civiform-build-tag'. If"
                          + " SHOW_CIVIFORM_IMAGE_TAG_ON_LANDING_PAGE is set to true, is also"
                          + " shown on the login page if CIVIFORM_VERSION is the empty string or"
                          + " set to 'latest'.",
                      SettingType.STRING),
                  SettingDescription.create(
                      "CIVIFORM_VERSION",
                      "The release version of CiviForm. For example: v1.18.0. If"
                          + " SHOW_CIVIFORM_IMAGE_TAG_ON_LANDING_PAGE is set to true, is also"
                          + " shown on the login page if it a value other than the empty string or"
                          + " 'latest'.",
                      SettingType.STRING),
                  SettingDescription.create(
                      "CLIENT_IP_TYPE",
                      "Where to find the IP address for incoming requests. Default is \"DIRECT\""
                          + " where the IP address of the request is the originating IP address."
                          + " If \"FORWARDED\" then request has been reverse proxied and the"
                          + " originating IP address is stored in the X-Forwarded-For header.",
                      SettingType.ENUM))));
}
