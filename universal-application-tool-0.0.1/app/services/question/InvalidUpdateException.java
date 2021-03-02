package services.question;

public class InvalidUpdateException extends Exception {
  public InvalidUpdateException(String reason) {
    super("Question update failed: " + reason);
  }
}
