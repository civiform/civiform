package services.question.exceptions;

/** QuestionNotFoundException is thrown when a question definition cannot be found. */
public class QuestionNotFoundException extends Exception {
  public QuestionNotFoundException(long questionId) {
    super(String.format("Question not found for ID: %d", questionId));
  }

  public QuestionNotFoundException(long questionId, long programId) {
    super(String.format("Question (ID %d) not found in Program (ID %d)", questionId, programId));
  }
}
