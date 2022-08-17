package services.program;

public class InvalidQuestionPositionException extends Exception {
  public InvalidQuestionPositionException(int position, int totalQuestions) {
    super(
        String.format(
            "Invalid question position %d. It must be between 0 and %d inclusive",
            position, totalQuestions - 1));
  }
}
