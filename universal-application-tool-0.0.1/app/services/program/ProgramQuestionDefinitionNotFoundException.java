package services.program;

/**
 * ProgramQuestionDefinitionNotFoundException is thrown when a question cannot be found in a program
 * but it is expected to.
 */
public class ProgramQuestionDefinitionNotFoundException extends Exception {
  public ProgramQuestionDefinitionNotFoundException(
      long programId, long blockDefinitionId, long questionDefinitionId) {
    super(
        String.format(
            "Question not found in Program (ID %d) Block (ID %d) for question (ID %d)",
            programId, blockDefinitionId, questionDefinitionId));
  }
}
