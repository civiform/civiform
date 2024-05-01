package auth;

import com.typesafe.config.Config;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Enum representing supported options for auth IDP for applicants and admins. */
public enum AuthIdentityProviderName {
  IDCS_APPLICANT("idcs"),
  LOGIN_RADIUS_APPLICANT("login-radius"),
  ADFS_ADMIN("adfs"),
  GENERIC_OIDC_ADMIN("generic-oidc-admin"),
  GENERIC_OIDC_APPLICANT("generic-oidc"),
  LOGIN_GOV_APPLICANT("login-gov"),
  AUTH0_APPLICANT("auth0"),
  DISABLED_APPLICANT("disabled");

  public static final String AUTH_APPLICANT_CONFIG_PATH = "civiform_applicant_idp";
  public static final String AUTH_ADMIN_CONFIG_PATH = "civiform_admin_idp";

  private final String authIdentityProviderNameString;

  AuthIdentityProviderName(String authIdentityProviderNameString) {
    this.authIdentityProviderNameString = authIdentityProviderNameString;
  }

  private static AuthIdentityProviderName getByName(String authIdentityProviderNameString) {
    for (AuthIdentityProviderName provider : AuthIdentityProviderName.values()) {
      if (provider.getValue().equals(authIdentityProviderNameString)) {
        return provider;
      }
    }
    throw new IllegalArgumentException(
        "No AuthIdentityProviderName found for '" + authIdentityProviderNameString + "'");
  }

  public static AuthIdentityProviderName applicantIdentityProviderfromConfig(Config config) {
    if (!config.hasPath(AUTH_APPLICANT_CONFIG_PATH)) {
      // return IDCS if no config is specified.
      return AuthIdentityProviderName.IDCS_APPLICANT;
    }
    String providerName = config.getString(AUTH_APPLICANT_CONFIG_PATH);
    try {
      return getByName(providerName);
    } catch (IllegalArgumentException e) {
      String supportedOptions =
          Arrays.stream(AuthIdentityProviderName.values())
              .map(AuthIdentityProviderName::getValue)
              .collect(Collectors.joining(", "));
      throw new IllegalArgumentException(
          "Unsupported civiform_applicant_idp value: "
              + providerName
              + ". Supported values are "
              + supportedOptions);
    }
  }

  public static AuthIdentityProviderName adminIdentityProviderfromConfig(Config config) {
    if (!config.hasPath(AUTH_ADMIN_CONFIG_PATH)) {
      // For a long time, this was the only identity provider for admins
      return AuthIdentityProviderName.ADFS_ADMIN;
    }
    String providerName = config.getString(AUTH_ADMIN_CONFIG_PATH);
    try {
      return getByName(providerName);
    } catch (IllegalArgumentException e) {
      String supportedOptions =
          Arrays.stream(AuthIdentityProviderName.values())
              .map(AuthIdentityProviderName::getValue)
              .collect(Collectors.joining(", "));
      throw new IllegalArgumentException(
          "Unsupported civiform_admin_idp value: "
              + providerName
              + ". Supported values are "
              + supportedOptions);
    }
  }

  /** Returns the string value associated with the enum. */
  public String getValue() {
    return authIdentityProviderNameString;
  }
}
