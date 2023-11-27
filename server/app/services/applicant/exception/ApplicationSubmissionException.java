package services.applicant.exception;

/**
 * ApplicationSubmissionException is thrown when an {@link models.ApplicationModel} fails to be
 * saved.
 */
public class ApplicationSubmissionException extends Exception {
  public ApplicationSubmissionException(long applicantId, long programId) {
    super(
        String.format(
            "Application for applicant %d and program %d failed to save", applicantId, programId));
  }
}
