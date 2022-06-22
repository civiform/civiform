package controllers.api;

/**
 * Thrown when an API request is authorized but malformed. This class is final to ensure that it
 * works with {@link controllers.ErrorHandler} and subclasses aren't thrown with the same expected
 * behavior.
 */
public final class BadApiRequestException extends RuntimeException {

  public BadApiRequestException(String message) {
    super(message);
  }
}
