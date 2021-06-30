package services.applicant.exception;

/**
 * ApplicantNotFoundException is thrown when the {@link models.Applicant} cannot be found by the
 * specified ID.
 */
public class ApplicantNotFoundException extends Exception {
  public ApplicantNotFoundException(long applicantId) {
    super(String.format("Applicant not found for ID %d", applicantId));
  }
}
