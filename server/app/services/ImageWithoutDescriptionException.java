package services;

/**
 * Exception thrown when an admin attempts to add an image without a description but isn't allowed
 * to.
 *
 * <p>The message provided will be shown to the admin as an error.
 */
public class ImageWithoutDescriptionException extends RuntimeException {
  public ImageWithoutDescriptionException(String message) {
    super(message);
  }
}
