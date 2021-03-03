package services.question;

public class QuestionNotFoundException extends Exception {
  public QuestionNotFoundException(long id) {
    super("Question not found for ID: " + id);
  }

  public QuestionNotFoundException(long questionId, long programId) {
    super(
        String.format(
            "Question not found for Question (ID %d) in Program (ID %d)", questionId, programId));
  }
}
