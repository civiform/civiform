package services.question.types;

import services.applicant.question.AddressQuestion;
import services.applicant.question.CurrencyQuestion;
import services.applicant.question.DateQuestion;
import services.applicant.question.EmailQuestion;
import services.applicant.question.EnumeratorQuestion;
import services.applicant.question.FileUploadQuestion;
import services.applicant.question.IdQuestion;
import services.applicant.question.MultiSelectQuestion;
import services.applicant.question.NameQuestion;
import services.applicant.question.NullQuestion;
import services.applicant.question.NumberQuestion;
import services.applicant.question.PhoneQuestion;
import services.applicant.question.Question;
import services.applicant.question.SingleSelectQuestion;
import services.applicant.question.StaticContentQuestion;
import services.applicant.question.TextQuestion;
import services.question.exceptions.InvalidQuestionTypeException;

/** Defines types of questions supported. */
public enum QuestionType {
  ADDRESS("Address", AddressQuestion.class),
  CHECKBOX("Checkbox", MultiSelectQuestion.class),
  CURRENCY("Currency", CurrencyQuestion.class),
  DATE("Date", DateQuestion.class),
  DROPDOWN("Dropdown", SingleSelectQuestion.class),
  EMAIL("Email", EmailQuestion.class),
  ENUMERATOR("Enumerator", EnumeratorQuestion.class),
  FILEUPLOAD("File Upload", FileUploadQuestion.class),
  ID("ID", IdQuestion.class),
  NAME("Name", NameQuestion.class),
  NUMBER("Number", NumberQuestion.class),
  RADIO_BUTTON("Radio Button", SingleSelectQuestion.class),
  STATIC("Static Text", StaticContentQuestion.class),
  TEXT("Text", TextQuestion.class),
  PHONE("Phone Number", PhoneQuestion.class),
  NULL_QUESTION("Missing Question", NullQuestion.class);

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
    for (QuestionType type : QuestionType.values()) {
      if (type.getLabel().equalsIgnoreCase(name)) {
        return type;
      }
    }
    throw new InvalidQuestionTypeException(name);
  }

  public String getLabel() {
    return this.label;
  }

  public Class<? extends Question> getSupportedQuestion() {
    return supportedQuestion;
  }
}
