package controllers.admin;

/**
 * Exception when a request is sent to modify something that can not be changed. This class is final
 * to ensure that it works with {@link controllers.ErrorHandler} and subclasses aren't thrown with
 * the same expected behavior.
 */
public final class NotChangeableException extends RuntimeException {
  public NotChangeableException(String message) {
    super(message);
  }
}
