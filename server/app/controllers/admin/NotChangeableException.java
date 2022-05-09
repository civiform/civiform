package controllers.admin;

class NotChangeableException extends RuntimeException {
  public NotChangeableException(String message) {
    super(message);
  }
}
