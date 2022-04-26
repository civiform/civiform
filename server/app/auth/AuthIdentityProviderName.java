package auth;

import java.util.Arrays;
import java.util.Optional;

/** Enum representing supported options for auth IDP for applicants and admins. */
public enum AuthIdentityProviderName {
  IDCS_APPLICANT("idcs"),
  LOGIN_RADIUS_APPLICANT("login-radius"),
  ADFS_ADMIN("adfs");

  private final String authIdentityProviderNameString;

  AuthIdentityProviderName(String authIdentityProviderNameString) {
    this.authIdentityProviderNameString = authIdentityProviderNameString;
  }

  /** Returns the enum associated with the provided string value. */
  public static Optional<AuthIdentityProviderName> forString(String string) {
    return Arrays.stream(AuthIdentityProviderName.values())
        .filter(provider -> provider.getString().equals(string))
        .findFirst();
  }

  /** Returns the string value associated with the enum. */
  public String getString() {
    return authIdentityProviderNameString;
  }
}
