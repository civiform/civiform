package services.question.types;

import services.question.exceptions.InvalidQuestionTypeException;

public enum QuestionType {
  ADDRESS(false),
  CHECKBOX(true),
  DATE(false),
  DROPDOWN(true),
  EMAIL(false),
  ENUMERATOR(false),
  FILEUPLOAD(false),
  NAME(false),
  NUMBER(false),
  PHONENUMBER(false),
  RADIO_BUTTON(true),
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
    String upperName = name.toUpperCase();
    try {
      return valueOf(upperName);
    } catch (IllegalArgumentException e) {
      throw new InvalidQuestionTypeException(upperName);
    }
  }
}
