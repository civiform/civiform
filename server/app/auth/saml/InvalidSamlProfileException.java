package auth.saml;

/** Raised when a SAML profile is valid. */
public final class InvalidSamlProfileException extends RuntimeException {

  InvalidSamlProfileException(String message) {
    super(message);
  }
}
