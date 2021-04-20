package services.program;

public class ProgramBlockNotFoundException extends Exception {
  public ProgramBlockNotFoundException(long programId, long blockDefinitionId) {
    super(
        "Block not found in Program (ID "
            + programId
            + ") for block definition ID "
            + blockDefinitionId);
  }

  public ProgramBlockNotFoundException(long programId, String blockId) {
    super("Block not found in Program (ID " + programId + ") for block ID " + blockId);
  }
}
