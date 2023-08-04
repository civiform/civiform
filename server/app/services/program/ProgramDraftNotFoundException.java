package services.program;

/**
 * ProgramDraftNotFoundException is thrown when a program does not have a draft found by the
 * specified identifier.
 */
public final class ProgramDraftNotFoundException extends Exception {
  public ProgramDraftNotFoundException(String slug) {
    super("Program draft not found for slug: " + slug);
  }
}
