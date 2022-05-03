package services.question.types;

import services.question.exceptions.InvalidQuestionTypeException;

/** Defines types of questions supported. */
public enum QuestionType {
  ADDRESS(false, "Address Field"),
  CHECKBOX(true, "Checkbox"),
  CURRENCY(false, "Currency Field"),
  DATE(false, "Date Picker"),
  DROPDOWN(true, "Dropdown"),
  EMAIL(false, "Email Field"),
  ENUMERATOR(false, "Enumerator"),
  FILEUPLOAD(false, "File Upload"),
  ID(false, "ID Field"),
  NAME(false, "Name Field"),
  NUMBER(false, "Number Field"),
  RADIO_BUTTON(true, "Radio Button"),
  STATIC(false, "Static Text"),
  TEXT(false, "Text Field");

  private final boolean isMultiOptionType;
  private final String typeString;

  QuestionType(boolean isMultiOptionType, String typeString) {
    this.isMultiOptionType = isMultiOptionType;
    this.typeString = typeString;
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

  public String getTypeString() {
    return this.typeString;
  }
}
