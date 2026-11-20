package auth.oidc;

/** Raised when an OidcProfile is invalid. */
public final class InvalidOidcProfileException extends RuntimeException {
  InvalidOidcProfileException(String message) {
    super(message);
  }
}
