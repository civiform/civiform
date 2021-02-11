package services.question;

public class InvalidPathException extends Exception {
  public InvalidPathException(String pathName) {
    super("Path not found: " + pathName);
  }
}
