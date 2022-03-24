package auth.saml;

/** Raised when a SAML profile is valid. */
public class InvalidSamlProfileException extends RuntimeException {

  InvalidSamlProfileException(String message) {
    super(message);
  }
}
