package services.program;

/**
 * {@link IllegalApiBridgeStateException} is thrown when a change to a program would cause a
 * breakage to an api bridge definition.
 */
public class IllegalApiBridgeStateException extends RuntimeException {

  public IllegalApiBridgeStateException(String message) {
    super(message);
  }
}
