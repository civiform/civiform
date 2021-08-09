package services.applicant.exception;

/**
 * ProgramBlockNotFoundException is thrown when a screen (block) cannot be found by the specified
 * ID.
 */
public class ProgramBlockNotFoundException extends Exception {
  public ProgramBlockNotFoundException(long programId, String blockId) {
    super("Block not found in Program (ID " + programId + ") for block ID " + blockId);
  }
}
