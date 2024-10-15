package services.applications;

public final class ApplicationNotFound extends Exception {
  public ApplicationNotFound(long applicationId) {
    super(
      String.format(
        "Application for the id %d, is not found.",
         applicationId));
  }
}
