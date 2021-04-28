package services.applicant;

public class ProgramBlockNotFoundException extends Exception {
  public ProgramBlockNotFoundException(long programId, String blockId) {
    super("Block not found in Program (ID " + programId + ") for block ID " + blockId);
  }
}
