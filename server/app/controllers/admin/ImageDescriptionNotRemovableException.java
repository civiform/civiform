package controllers.admin;

/**
 * Exception thrown when an admin attempts to remove the summary image description but isn't allowed
 * to.
 *
 * <p>The message provided will be shown to the admin as an error.
 */
public class ImageDescriptionNotRemovableException extends RuntimeException {
  public ImageDescriptionNotRemovableException(String message) {
    super(message);
  }
}
