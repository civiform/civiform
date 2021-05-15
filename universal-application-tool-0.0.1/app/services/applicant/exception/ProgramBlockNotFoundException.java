package services.applicant.exception;

public class ProgramBlockNotFoundException extends Exception {
  public ProgramBlockNotFoundException(long programId, String blockId) {
    super("Block not found in Program (ID " + programId + ") for block ID " + blockId);
  }
}
