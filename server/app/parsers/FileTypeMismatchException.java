package parsers;

/** Thrown when an uploaded file's actual content does not match its declared content type. */
public class FileTypeMismatchException extends RuntimeException {
  public FileTypeMismatchException(String message) {
    super(message);
  }
}
