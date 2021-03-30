package services.question;

public enum QuestionType {
  ADDRESS(false),
  CHECKBOX(true),
  DROPDOWN(true),
  NAME(false),
  NUMBER(false),
  REPEATER(false),
  TEXT(false);

  private final boolean isMultiOptionType;

  QuestionType(boolean isMultiOptionType) {
    this.isMultiOptionType = isMultiOptionType;
  }

  /**
   * Returns true if this question type supports multiple options (that is, the applicant must
   * select between multiple, pre-defined answer options). Returns false otherwise.
   */
  public boolean isMultiOptionType() {
    return this.isMultiOptionType;
  }

  public static QuestionType of(String name) throws InvalidQuestionTypeException {
    try {
      return valueOf(name);
    } catch (IllegalArgumentException e) {
      throw new InvalidQuestionTypeException(name);
    }
  }
}
