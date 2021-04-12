package services.program;

public class ProgramBlockNotFoundException extends Exception {
  public ProgramBlockNotFoundException(long programId, long blockId) {
    super("Block not found in Program (ID " + programId + ") for block ID " + blockId);
  }

  public ProgramBlockNotFoundException(long programId, String blockId) {
    super("Block not found in Program (ID " + programId + ") for block ID " + blockId);
  }
}
