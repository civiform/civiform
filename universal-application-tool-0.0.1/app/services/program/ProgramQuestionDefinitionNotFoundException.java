package services.program;

public class ProgramQuestionDefinitionNotFoundException extends Exception {
  public ProgramQuestionDefinitionNotFoundException(
      long programId, long blockDefinitionId, long questionDefinitionId) {
    super(
        String.format(
            "Question not found in Program (ID %d) Block (ID %d) for question (ID %d)",
            programId, blockDefinitionId, questionDefinitionId));
  }
}
