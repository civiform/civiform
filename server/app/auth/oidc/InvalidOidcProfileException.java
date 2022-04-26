package auth.oidc;

/** Raised when an OidcProfile is invalid. */
public class InvalidOidcProfileException extends RuntimeException {
  InvalidOidcProfileException(String message) {
    super(message);
  }
}
