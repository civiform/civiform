package services.statuses;

/**
 * DuplicateStatusException is thrown when an {@link StatusDefinitions.Status} with the same
 * statusText already exists in the list of a program's configured statuses for application review.
 */
public final class DuplicateStatusException extends Exception {
  private final String userFacingMessage;

  public DuplicateStatusException(String statusName) {
    super(makeUserFacingMessage(statusName));
    this.userFacingMessage = makeUserFacingMessage(statusName);
  }

  private static String makeUserFacingMessage(String statusName) {
    return String.format("A status with name %s already exists", statusName);
  }

  /** Returns an error message suitable for displaying to an end-user. */
  public String userFacingMessage() {
    return this.userFacingMessage;
  }
}
