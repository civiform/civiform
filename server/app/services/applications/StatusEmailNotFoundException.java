package services.applications;

/** Exception for when an Application Status has no email but one is used. */
public final class StatusEmailNotFoundException extends Exception {
  public StatusEmailNotFoundException(String status, long programId) {
    super(
        String.format(
            "Email for status (%s) on program id %d was requested but does not exist.",
            status, programId));
  }
}
