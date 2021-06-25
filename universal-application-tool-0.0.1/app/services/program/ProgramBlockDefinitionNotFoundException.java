package services.program;

public class ProgramBlockDefinitionNotFoundException extends Exception {
  public ProgramBlockDefinitionNotFoundException(long programId, long blockDefinitionId) {
    super(
        "Screen not found in Program (ID "
            + programId
            + ") for screen definition ID "
            + blockDefinitionId);
  }
}
