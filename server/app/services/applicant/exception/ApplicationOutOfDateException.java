package services.applicant.exception;

/** Indicates that an application is out of date somehow. */
public class ApplicationOutOfDateException extends Exception {
  public ApplicationOutOfDateException(String message) {
    super(message);
  }
}
