package services.applicant.exception;

/** Indicates that an application is out of date somehow. */
public class ApplicationOutOfDateException extends RuntimeException {
  public ApplicationOutOfDateException() {
    super();
  }
}
