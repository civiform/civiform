package services.cloud.aws;

public final class DeletionFailedException extends Exception {
    public DeletionFailedException(Exception triggeringException) {
        super(String.format("Deletion failed due to: %s", triggeringException));
    }
}
