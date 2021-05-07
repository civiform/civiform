package services.applicant.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import services.Path;
import services.applicant.ApplicantData;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.question.types.ScalarType;

/**
 * Represents a question in the context of a specific applicant. Other type-specific classes (e.g.
 * {@link NameQuestion}) use this class's data to represent a specific question type. These other
 * classes provide access to the applicant's answer for the question. They can also implement
 * server-side validation logic.
 */
public class ApplicantQuestion {

  private final QuestionDefinition questionDefinition;
  private final Path contextualizedPath;
  private final ApplicantData applicantData;

  public ApplicantQuestion(
      QuestionDefinition questionDefinition, ApplicantData applicantData, Path contextualizedPath) {
    this.questionDefinition = checkNotNull(questionDefinition);
    this.applicantData = checkNotNull(applicantData);
    this.contextualizedPath = checkNotNull(contextualizedPath);
  }

  protected ApplicantData getApplicantData() {
    return this.applicantData;
  }

  public QuestionDefinition getQuestionDefinition() {
    return this.questionDefinition;
  }

  public QuestionType getType() {
    return questionDefinition.getQuestionType();
  }

  public String getQuestionText() {
    return questionDefinition.getQuestionTextOrDefault(applicantData.preferredLocale());
  }

  public String getQuestionHelpText() {
    return questionDefinition.getQuestionHelpTextOrDefault(applicantData.preferredLocale());
  }

  /**
   * Returns the contextualized path for this question. The path is contextualized with respect to
   * the enumerated elements it is about.
   *
   * <p>For example, a generic path about the name of an applicant's household member may look like
   * "applicant.household_member[].name", while a contextualized path would look like
   * "applicant.household_member[3].name".
   */
  public Path getContextualizedPath() {
    return contextualizedPath.join(questionDefinition.getQuestionPathSegment());
  }

  /**
   * Returns the map of contextualized paths to scalars and their {@link ScalarType}s used by this
   * question. This includes metadata paths.
   *
   * <p>This should not be used for {@link QuestionType#ENUMERATOR} questions.
   */
  public ImmutableMap<Path, ScalarType> getContextualizedScalars() {
    try {
      return ImmutableMap.<Scalar, ScalarType>builder()
          .putAll(Scalar.getScalars(getType()))
          .putAll(Scalar.getMetadataScalars())
          .build()
          .entrySet()
          .stream()
          .collect(
              ImmutableMap.toImmutableMap(
                  entry -> getContextualizedPath().join(entry.getKey()), Map.Entry::getValue));
    } catch (InvalidQuestionTypeException | UnsupportedQuestionTypeException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean hasErrors() {
    return errorsPresenter().hasQuestionErrors() || errorsPresenter().hasTypeSpecificErrors();
  }

  public Optional<Long> getUpdatedInProgramMetadata() {
    Path contextualizedMetadataPath = getContextualizedPath().join(Scalar.PROGRAM_UPDATED_IN);

    // Metadata for enumerators is stored for each JSON array element, but we rely on metadata for
    // the first one.
    if (questionDefinition.isEnumerator()) {
      contextualizedMetadataPath =
          getContextualizedPath().atIndex(0).join(Scalar.PROGRAM_UPDATED_IN);
    }

    return applicantData.readLong(contextualizedMetadataPath);
  }

  public Optional<Long> getLastUpdatedTimeMetadata() {
    Path contextualizedMetadataPath = getContextualizedPath().join(Scalar.UPDATED_AT);

    // Metadata for enumerators are stored for each JSON array element, but we rely on metadata for
    // the first one.
    if (questionDefinition.isEnumerator()) {
      contextualizedMetadataPath = getContextualizedPath().atIndex(0).join(Scalar.UPDATED_AT);
    }

    return applicantData.readLong(contextualizedMetadataPath);
  }

  public AddressQuestion createAddressQuestion() {
    return new AddressQuestion(this);
  }

  public DateQuestion createDateQuestion() {
    return new DateQuestion(this);
  }

  public FileUploadQuestion createFileUploadQuestion() {
    return new FileUploadQuestion(this);
  }

  public MultiSelectQuestion createMultiSelectQuestion() {
    return new MultiSelectQuestion(this);
  }

  public NameQuestion createNameQuestion() {
    return new NameQuestion(this);
  }

  public NumberQuestion createNumberQuestion() {
    return new NumberQuestion(this);
  }

  public EnumeratorQuestion createEnumeratorQuestion() {
    return new EnumeratorQuestion(this);
  }

  public SingleSelectQuestion createSingleSelectQuestion() {
    return new SingleSelectQuestion(this);
  }

  public TextQuestion createTextQuestion() {
    return new TextQuestion(this);
  }

  public PresentsErrors errorsPresenter() {
    switch (getType()) {
      case ADDRESS:
        return createAddressQuestion();
      case CHECKBOX:
        return createMultiSelectQuestion();
      case DATE:
        return createDateQuestion();
      case FILEUPLOAD:
        return createFileUploadQuestion();
      case NAME:
        return createNameQuestion();
      case NUMBER:
        return createNumberQuestion();
      case DROPDOWN: // fallthrough to RADIO_BUTTON
      case RADIO_BUTTON:
        return createSingleSelectQuestion();
      case ENUMERATOR:
        return createEnumeratorQuestion();
      case TEXT:
        return createTextQuestion();
      default:
        throw new RuntimeException("Unrecognized question type: " + getType());
    }
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof ApplicantQuestion) {
      ApplicantQuestion that = (ApplicantQuestion) object;
      return this.questionDefinition.equals(that.questionDefinition)
          && this.applicantData.equals(that.applicantData);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(questionDefinition, applicantData);
  }
}
