package services.apibridge;

/** Thrown if an error occurs when the api bridge fails and throws a processing error */
public final class ApiBridgeProcessingException extends RuntimeException {
  public ApiBridgeProcessingException(String message) {
    super(message);
  }

  public ApiBridgeProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
