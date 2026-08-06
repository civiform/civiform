package parsers;

/** Thrown when an uploaded file fails type validation. */
public class FileUploadTypeException extends RuntimeException {
  public FileUploadTypeException(String message) {
    super(message);
  }
}
