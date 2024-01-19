package services.cloud.aws;

/** Exception for when a request to list all the files in cloud storage has failed. */
public final class FileListFailureException extends Exception {
  public FileListFailureException(Exception originalException) {
    super("Unable to list files", originalException);
  }
}
