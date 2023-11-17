package services.program;

/**
 * This exception is thrown when an image description translation is provided, but the program
 * doesn't have an image description.
 */
public final class OutOfDateImageDescriptionException extends Exception {
  private static final String USER_FACING_MESSAGE =
      "The program's image description is out of date.";

  public OutOfDateImageDescriptionException() {
    super(USER_FACING_MESSAGE);
  }

  /** Returns an error message suitable for displaying to an end-user. */
  public String userFacingMessage() {
    return USER_FACING_MESSAGE;
  }
}
