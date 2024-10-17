package services.applications;

public final class ApplicationNotFoundException extends Exception {
  public ApplicationNotFoundException(long applicationId) {
    super(String.format("Application for the id %d, is not found.", applicationId));
  }
}
