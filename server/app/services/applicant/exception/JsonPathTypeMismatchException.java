package services.applicant.exception;

import services.Path;

/**
 * Thrown when the expected scalar type for a given JSON path does not match the actual value at
 * that path.
 */
public class JsonPathTypeMismatchException extends Exception {
  public JsonPathTypeMismatchException(Path path, Class type, Throwable cause) {
    super(path + " does not have expected type " + type, cause);
  }
}
