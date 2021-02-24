package services.question;

public class QuestionNotFoundException extends Exception {
  public QuestionNotFoundException(long id) {
    super("Question not found for ID: " + id);
  }
}
