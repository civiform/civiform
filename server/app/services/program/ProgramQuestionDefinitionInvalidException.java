package services.program;

/**
 * ProgramQuestionDefinitionInvalidException is thrown when the specified block contains a question
 * with parameters it should not have, such as address correction being enabled for multiple address
 * questions.
 */
public class ProgramQuestionDefinitionInvalidException extends Exception {
  public ProgramQuestionDefinitionInvalidException(
      long programId, long blockDefinitionId, long questionId) {
    super(
        "Block ID "
            + blockDefinitionId
            + " in Program ID "
            + programId
            + " contains invalid question ID "
            + questionId);
  }
}
