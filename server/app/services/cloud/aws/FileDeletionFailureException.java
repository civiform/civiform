package services.cloud.aws;

/** Exception for when a request to delete a file from cloud storage has failed. */
public final class FileDeletionFailureException extends Exception {
  public FileDeletionFailureException(Exception originalException) {
    super("The deletion was unable to be processed", originalException);
  }
}
