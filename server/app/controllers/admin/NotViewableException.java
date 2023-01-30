package controllers.admin;

/**
 * Exception when a request is sent to view something that can not be viewed/shown. This class is
 * final to ensure that it works with {@link controllers.ErrorHandler} and subclasses aren't thrown
 * with the same expected behavior.
 */
public final class NotViewableException extends RuntimeException {
  public NotViewableException(String message) {
    super(message);
  }
}
