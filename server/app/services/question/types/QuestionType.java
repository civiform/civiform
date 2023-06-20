package services.question.types;

import java.util.Locale;
import services.applicant.question.*;
import services.question.exceptions.InvalidQuestionTypeException;

/** Defines types of questions supported. */
public enum QuestionType {
  ADDRESS("Address Field", AddressQuestion.class),
  CHECKBOX("Checkbox", MultiSelectQuestion.class),
  CURRENCY("Currency Field", CurrencyQuestion.class),
  DATE("Date Picker", DateQuestion.class),
  DROPDOWN("Dropdown", SingleSelectQuestion.class),
  EMAIL("Email Field", EmailQuestion.class),
  ENUMERATOR("Enumerator", EnumeratorQuestion.class),
  FILEUPLOAD("File Upload", FileUploadQuestion.class),
  ID("ID Field", IdQuestion.class),
  NAME("Name Field", NameQuestion.class),
  NUMBER("Number Field", NumberQuestion.class),
  RADIO_BUTTON("Radio Button", SingleSelectQuestion.class),
  STATIC("Static Text", StaticContentQuestion.class),
  TEXT("Text Field", TextQuestion.class),
  PHONE("Phone Field", PhoneQuestion.class);

  private final String label;
  private final Class<? extends Question> supportedQuestion;

  QuestionType(String label, Class<? extends Question> supportedQuestion) {
    this.label = label;
    this.supportedQuestion = supportedQuestion;
  }

  /**
   * Returns true if this question type supports multiple options (that is, the applicant must
   * select between multiple, pre-defined answer options). Returns false otherwise.
   */
  public boolean isMultiOptionType() {
    return getSupportedQuestion() == SingleSelectQuestion.class
        || getSupportedQuestion() == MultiSelectQuestion.class;
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
