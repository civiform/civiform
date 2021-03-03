package services.question;

public class QuestionNotFoundException extends Exception {
  public QuestionNotFoundException(long id) {
    super("Question not found for ID: " + id);
  }

  public QuestionNotFoundException(long questionId, long programId) {
    super(
        String.format(
            "Question (ID %d) not found in Program (ID %d)", questionId, programId));
  }
}
