package services.question;

public enum QuestionType {
  ADDRESS,
  NAME,
  NUMBER,
  REPEATER,
  TEXT;

  public static QuestionType of(String name) throws InvalidQuestionTypeException {
    String upperName = name.toUpperCase();
    try {
      return valueOf(upperName);
    } catch (IllegalArgumentException e) {
      throw new InvalidQuestionTypeException(upperName);
    }
  }
}
