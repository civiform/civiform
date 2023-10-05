package services.applicant.exception;

/**
 * Thrown if an application is submitted that is a duplicate of a previously submitted one Triggers
 * a submit application confirmation modal to be shown to the applicant.
 */
public class DuplicateApplicationException extends RuntimeException {
  public DuplicateApplicationException() {
    super();
  }
}
