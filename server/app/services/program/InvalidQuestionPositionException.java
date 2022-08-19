package services.program;

public final class InvalidQuestionPositionException extends Exception {
  private InvalidQuestionPositionException(String message) {
    super(message);
  }

  public static InvalidQuestionPositionException positionOutOfBounds(
      int position, int totalQuestions) {
    return new InvalidQuestionPositionException(
        String.format(
            "Invalid question position %d. It must be between 0 and %d inclusive",
            position, totalQuestions - 1));
  }

  public static InvalidQuestionPositionException missingPositionArgument() {
    return new InvalidQuestionPositionException(
        "request missing 'position' value or its not a number");
  }
}
