package services.question.types;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Locale;
import services.applicant.question.AbstractQuestion;
import services.applicant.question.AddressQuestion;
import services.applicant.question.CurrencyQuestion;
import services.applicant.question.DateQuestion;
import services.applicant.question.EmailQuestion;
import services.applicant.question.EnumeratorQuestion;
import services.applicant.question.FileUploadQuestion;
import services.applicant.question.IdQuestion;
import services.applicant.question.MapQuestion;
import services.applicant.question.MultiSelectQuestion;
import services.applicant.question.NameQuestion;
import services.applicant.question.NullQuestion;
import services.applicant.question.NumberQuestion;
import services.applicant.question.PhoneQuestion;
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
  MAP("Map", MapQuestion.class),
  NAME("Name", NameQuestion.class),
  NUMBER("Number", NumberQuestion.class),
  PHONE("Phone Number", PhoneQuestion.class),
  RADIO_BUTTON("Radio Button", SingleSelectQuestion.class),
  STATIC("Static Text", StaticContentQuestion.class),
  TEXT("Text", TextQuestion.class),
  YES_NO("Yes/No", SingleSelectQuestion.class),
  NULL_QUESTION("Missing Question", NullQuestion.class);

  private final String label;
  private final Class<? extends AbstractQuestion> supportedQuestion;

  private static final List<QuestionType> QUESTION_TYPES_SUPPORTING_SETTINGS =
      List.of(QuestionType.MAP);

  private static final ImmutableSet<QuestionType> QUESTION_TYPES_SUPPORTING_OPTION_SCORES =
      ImmutableSet.of(QuestionType.CHECKBOX, QuestionType.DROPDOWN, QuestionType.RADIO_BUTTON);

  QuestionType(String label, Class<? extends AbstractQuestion> supportedQuestion) {
    this.label = label;
    this.supportedQuestion = supportedQuestion;
  }

  /**
   * Determines if a {@link QuestionType} supports Question Settings
   *
   * @param questionType a {@link QuestionType}
   * @return boolean to indicate whether the question type supports settings
   */
  public static boolean supportsQuestionSettings(QuestionType questionType) {
    return QUESTION_TYPES_SUPPORTING_SETTINGS.contains(questionType);
  }

  /**
   * Determines if a {@link QuestionType} supports optional scores on its answer options.
   *
   * <p>This is deliberately narrower than {@link #isMultiOptionType()}: Yes/No questions are
   * multi-option but must never carry scores.
   *
   * @param questionType a {@link QuestionType}
   * @return boolean to indicate whether the question type supports option scores
   */
  public static boolean supportsOptionScores(QuestionType questionType) {
    return QUESTION_TYPES_SUPPORTING_OPTION_SCORES.contains(questionType);
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
    // Match naive string, e.g. "PHONE" -> PHONE
    String upperName = name.toUpperCase(Locale.ROOT);
    try {
      return valueOf(upperName);
    } catch (IllegalArgumentException e) {
      throw new InvalidQuestionTypeException(upperName);
    }
  }

  public static QuestionType fromLabel(String label) throws InvalidQuestionTypeException {
    // Match label, e.g. "Phone Number" -> PHONE
    for (QuestionType type : QuestionType.values()) {
      if (type.getLabel().equalsIgnoreCase(label)) {
        return type;
      }
    }
    throw new InvalidQuestionTypeException(label);
  }

  public String getLabel() {
    return this.label;
  }

  public Class<? extends AbstractQuestion> getSupportedQuestion() {
    return supportedQuestion;
  }
}
