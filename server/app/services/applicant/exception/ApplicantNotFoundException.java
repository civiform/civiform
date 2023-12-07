package services.applicant.exception;

/**
 * ApplicantNotFoundException is thrown when the {@link models.ApplicantModel} cannot be found by
 * the specified ID.
 */
public final class ApplicantNotFoundException extends Exception {
  public ApplicantNotFoundException(long applicantId) {
    super(String.format("Applicant not found for ID %d", applicantId));
  }
}
