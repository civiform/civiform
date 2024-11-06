package services.applications;

public class ApplicationAlreadyInStatusException extends Exception {
  ApplicationAlreadyInStatusException(Long applicationId, String status) {
    super(
        String.format(
            "Application for the id %d, is already in the status %s.", applicationId, status));
  }
}
