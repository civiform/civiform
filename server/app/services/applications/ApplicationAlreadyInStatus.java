package services.applications;

public class ApplicationAlreadyInStatus extends RuntimeException {
  ApplicationAlreadyInStatus(Long applicationId, String status) {
    super(
        String.format(
            "Application for the id %d, is in already in the status %s.", applicationId, status));
  }
}
