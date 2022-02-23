package services.program;

/**
 * ProgramBlockDefinitionNotFoundException is thrown when the specified block definition is not
 * found in this program.
 */
public class ProgramBlockDefinitionNotFoundException extends Exception {
  public ProgramBlockDefinitionNotFoundException(long programId, long blockDefinitionId) {
    super(
        "Block not found in Program (ID "
            + programId
            + ") for block definition ID "
            + blockDefinitionId);
  }
}
