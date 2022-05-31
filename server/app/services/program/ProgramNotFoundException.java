package services.program;

/**
 * ProgramNotFoundException is thrown when a program cannot be found by the specified identifier.
 * This class is final to ensure that it works with {@link controllers.ErrorHandler} and subclasses
 * aren't * thrown with the same expected behavior.
 */
public final class ProgramNotFoundException extends Exception {
  public ProgramNotFoundException(String slug) {
    super("Program not found for slug: " + slug);
  }

  public ProgramNotFoundException(long id) {
    super("Program not found for ID: " + id);
  }
}
