package services.question.types;

import java.util.Locale;
import services.applicant.question.*;
import services.question.exceptions.InvalidQuestionTypeException;

/** Defines types of questions supported. */
public enum QuestionType {
  ADDRESS(false, "Address Field", AddressQuestion.class),
  CHECKBOX(true, "Checkbox", MultiSelectQuestion.class),
  CURRENCY(false, "Currency Field", CurrencyQuestion.class),
  DATE(false, "Date Picker", DateQuestion.class),
  DROPDOWN(true, "Dropdown", SingleSelectQuestion.class),
  EMAIL(false, "Email Field", EmailQuestion.class),
  ENUMERATOR(false, "Enumerator", EnumeratorQuestion.class),
  FILEUPLOAD(false, "File Upload", FileUploadQuestion.class),
  ID(false, "ID Field", IdQuestion.class),
  NAME(false, "Name Field", NameQuestion.class),
  NUMBER(false, "Number Field", NumberQuestion.class),
  RADIO_BUTTON(true, "Radio Button", SingleSelectQuestion.class),
  STATIC(false, "Static Text", StaticContentQuestion.class),
  TEXT(false, "Text Field", TextQuestion.class),
  PHONE(false, "Phone Field", PhoneQuestion.class);

  private final boolean isMultiOptionType;
  private final String label;
  private final Class<? extends Question> supportedQuestion;

  QuestionType(
      boolean isMultiOptionType, String label, Class<? extends Question> supportedQuestion) {
    this.isMultiOptionType = isMultiOptionType;
    this.label = label;
    this.supportedQuestion = supportedQuestion;
  }

  /**
   * Returns true if this question type supports multiple options (that is, the applicant must
   * select between multiple, pre-defined answer options). Returns false otherwise.
   */
  public boolean isMultiOptionType() {
    return this.isMultiOptionType;
  }

  public static QuestionType of(String name) throws InvalidQuestionTypeException {
    String upperName = name.toUpperCase(Locale.ROOT);
    try {
      return valueOf(upperName);
    } catch (IllegalArgumentException e) {
      throw new InvalidQuestionTypeException(upperName);
    }
  }

  public String getLabel() {
    return this.label;
  }

  public Class<? extends Question> getSupportedQuestion() {
    return supportedQuestion;
  }
}
