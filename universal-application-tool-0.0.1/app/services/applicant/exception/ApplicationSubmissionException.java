package services.applicant.exception;

public class ApplicationSubmissionException extends Exception {
  public ApplicationSubmissionException(long applicantId, long programId) {
    super(
        String.format(
            "Application for applicant %d and program %d failed to save", applicantId, programId));
  }
}
