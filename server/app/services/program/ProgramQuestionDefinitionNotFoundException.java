package services.program;

/** Thrown when a question cannot be found in a program. */
public class ProgramQuestionDefinitionNotFoundException extends Exception {
  public ProgramQuestionDefinitionNotFoundException(
      long programId, long blockDefinitionId, long questionDefinitionId) {
    super(
        String.format(
            "Question not found in Program (ID %d) Block (ID %d) for question (ID %d)",
            programId, blockDefinitionId, questionDefinitionId));
  }

  public ProgramQuestionDefinitionNotFoundException(long programId, long questionDefinitionId) {
    super(
        String.format(
            "Question not found in Program (ID %d) for question (ID %d)",
            programId, questionDefinitionId));
  }
}
