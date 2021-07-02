package services.program;

/**
 * ProgramNotFoundException is thrown when a program cannot be found by the specified identifier.
 */
public class ProgramNotFoundException extends Exception {
  public ProgramNotFoundException(String slug) {
    super("Program not found for slug: " + slug);
  }

  public ProgramNotFoundException(long id) {
    super("Program not found for ID: " + id);
  }
}
