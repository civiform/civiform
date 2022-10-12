package auth;

import com.typesafe.config.Config;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Enum representing supported options for auth IDP for applicants and admins. */
public enum AuthIdentityProviderName {
  IDCS_APPLICANT("idcs"),
  LOGIN_RADIUS_APPLICANT("login-radius"),
  ADFS_ADMIN("adfs"),
  GENERIC_OIDC_APPLICANT("generic-oidc"),
  LOGIN_GOV_APPLICANT("login-gov"),
  AUTH0_APPLICANT("auth0"),
  DISABLED_APPLICANT("disabled");

  public static String AUTH_APPLICANT_CONFIG_PATH = "auth.applicant_idp";

  private final String authIdentityProviderNameString;

  AuthIdentityProviderName(String authIdentityProviderNameString) {
    this.authIdentityProviderNameString = authIdentityProviderNameString;
  }

  public static AuthIdentityProviderName fromConfig(Config config) {
    if (!config.hasPath(AUTH_APPLICANT_CONFIG_PATH)) {
      // return IDCS if no config is specified.
      return AuthIdentityProviderName.IDCS_APPLICANT;
    }
    String providerName = config.getString(AUTH_APPLICANT_CONFIG_PATH);
    for (var provider : AuthIdentityProviderName.values()) {
      if (provider.getString().equals(providerName)) {
        return provider;
      }
    }
    String supportedOptions =
        Arrays.stream(AuthIdentityProviderName.values())
            .map(AuthIdentityProviderName::getString)
            .collect(Collectors.joining(", "));
    throw new IllegalArgumentException(
        "Unsupported auth.applicant_idp value: "
            + providerName
            + ". Supported values are "
            + supportedOptions);
  }

  /** Returns the string value associated with the enum. */
  public String getString() {
    return authIdentityProviderNameString;
  }
}
