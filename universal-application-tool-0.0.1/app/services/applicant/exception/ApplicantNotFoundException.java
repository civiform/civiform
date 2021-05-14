package services.applicant.exception;

public class ApplicantNotFoundException extends Exception {
  public ApplicantNotFoundException(long applicantId) {
    super(String.format("Applicant not found for ID %d", applicantId));
  }
}
