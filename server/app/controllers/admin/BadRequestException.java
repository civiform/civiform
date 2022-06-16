package controllers.admin;

/**
 * Thrown when a request is malformed. This class is final to ensure that it works with {@link
 * controllers.ErrorHandler} and subclasses aren't thrown with the same expected behavior.
 */
public final class BadRequestException extends RuntimeException {

  public BadRequestException(String message) {
    super(message);
  }
}
