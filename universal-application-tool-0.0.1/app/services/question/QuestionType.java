package services.question;

public enum QuestionType {
  ADDRESS,
  NAME,
  NUMBER,
  REPEATER,
  SINGLE_SELECT,
  TEXT;

  public static QuestionType of(String name) throws InvalidQuestionTypeException {
    try {
      return valueOf(name);
    } catch (IllegalArgumentException e) {
      throw new InvalidQuestionTypeException(name);
    }
  }
}
