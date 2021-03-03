package services.program;

public class DuplicateProgramQuestionException extends Exception {
  public DuplicateProgramQuestionException(long programId, long questionId) {
    super(
        String.format("Question (ID %d) already exists in Program (ID %d)", questionId, programId));
  }
}
