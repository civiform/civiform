package services.applicant.exception;

/** Represents an application that has not met its programs eligibility conditions. */
public class ApplicationNotEligibleException extends RuntimeException {
  public ApplicationNotEligibleException() {
    super();
  }
}
