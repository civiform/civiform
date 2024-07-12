package services.applicationstatuses;

import services.program.StatusDefinitions;

/**
 * OutOfDateStatusesException is thrown when a program's list of {@link StatusDefinitions.Status} is
 * inconsistent with the provided set of statuses to update.
 */
public final class OutOfDateStatusesException extends Exception {
  private static final String USER_FACING_MESSAGE =
      "The program's associated statuses are out of date.";

  public OutOfDateStatusesException() {
    super(USER_FACING_MESSAGE);
  }

  /** Returns an error message suitable for displaying to an end-user. */
  public String userFacingMessage() {
    return USER_FACING_MESSAGE;
  }
}
