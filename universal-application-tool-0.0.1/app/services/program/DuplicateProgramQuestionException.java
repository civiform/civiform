package services.program;

/**
 * DuplicateProgramQuestionException is thrown when a program contains the same question more than
 * once.
 */
public class DuplicateProgramQuestionException extends Exception {
  public DuplicateProgramQuestionException(long programId, long questionId) {
    super(
        String.format("Question (ID %d) already exists in Program (ID %d)", questionId, programId));
  }
}
