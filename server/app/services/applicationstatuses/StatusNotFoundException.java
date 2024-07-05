package services.applicationstatuses;

/**
 * Represents when a program status string is used but not actually part of the program's
 * definition.
 */
public final class StatusNotFoundException extends Exception {
  public StatusNotFoundException(String status, long programId) {
    super(String.format("status (%s) is not valid for program id %d", status, programId));
  }
}
