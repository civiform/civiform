package controllers.admin;

/** Exception when a request is sent to modify something that can not be changed. */
public class NotChangeableException extends RuntimeException {
  public NotChangeableException(String message) {
    super(message);
  }
}
