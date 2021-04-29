package services.program;

public class ProgramBlockDefinitionNotFoundException extends Exception {
  public ProgramBlockDefinitionNotFoundException(long programId, long blockDefinitionId) {
    super(
        "Block not found in Program (ID "
            + programId
            + ") for block definition ID "
            + blockDefinitionId);
  }
}
