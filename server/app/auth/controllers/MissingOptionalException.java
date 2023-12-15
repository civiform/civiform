package auth.controllers;

/** Exception that represents a missing Optional value. */
public final class MissingOptionalException extends RuntimeException {
  public <T> MissingOptionalException(Class<T> clazz) {
    super(String.format("Required %s is missing", clazz.getName()));
  }
}
