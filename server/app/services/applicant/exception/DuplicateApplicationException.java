package services.applicant.exception;

/**
 * Thrown if an application is submitted that is a duplicate of a previously submitted one. Triggers
 * a screen to be shown to the applicant asking them to continue editing or exit the application.
 */
public class DuplicateApplicationException extends RuntimeException {
  public DuplicateApplicationException() {
    super();
  }
}
