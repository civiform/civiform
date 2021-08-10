package services.applicant.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.RepeatedEntity;
import services.applicant.ValidationErrorMessage;
import services.program.ProgramQuestionDefinition;
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

  private final ProgramQuestionDefinition programQuestionDefinition;
  private final ApplicantData applicantData;
  private final Optional<RepeatedEntity> repeatedEntity;

  /**
   * If this is a repeated question, it should be created with the repeated entity associated with
   * this question. If this is not a repeated question, then it should be created with an {@code
   * Optional.empty()} repeated entity.
   */
  public ApplicantQuestion(
      ProgramQuestionDefinition programQuestionDefinition,
      ApplicantData applicantData,
      Optional<RepeatedEntity> repeatedEntity) {
    this.programQuestionDefinition = checkNotNull(programQuestionDefinition);
    this.applicantData = checkNotNull(applicantData);
    this.repeatedEntity = checkNotNull(repeatedEntity);
  }

  /**
   * DEPRECATED.
   *
   * <p>This constructor is only used in tests, which should eventually be converted to use the
   * constructor that uses {@link ProgramQuestionDefinition}.
   */
  public ApplicantQuestion(
      QuestionDefinition questionDefinition,
      ApplicantData applicantData,
      Optional<RepeatedEntity> repeatedEntity) {
    this.programQuestionDefinition =
        ProgramQuestionDefinition.create(checkNotNull(questionDefinition), Optional.empty());
    this.applicantData = checkNotNull(applicantData);
    this.repeatedEntity = checkNotNull(repeatedEntity);
  }

  protected ApplicantData getApplicantData() {
    return this.applicantData;
  }

  public QuestionDefinition getQuestionDefinition() {
    return this.programQuestionDefinition.getQuestionDefinition();
  }

  public QuestionType getType() {
    return getQuestionDefinition().getQuestionType();
  }

  public boolean isOptional() {
    return programQuestionDefinition.optional();
  }

  /**
   * Return true if this question is answered, or left unanswered in the program specified.
   * Questions can only be left unanswered if they are optional.
   *
   * <p>For every applicant question, there are three possible states:
   *
   * <ol>
   *   <li>unvisited - the applicant has not seen this question yet
   *   <li>unanswered - the applicant has visited the question but chose to leave it unanswered.
   *       Only optional questions can be left unanswered
   *   <li>answered - the applicant has visited and provided an answer to the question
   * </ol>
   *
   * For unanswered optional questions, we care in which program it was left unanswered. For the
   * program which the optional question was left unanswered, it counts as "completed" for that
   * program.
   *
   * @return true if this question is answered, or left unanswered in the program specified.
   */
  public boolean isAnsweredOrLeftUnansweredInProgram() {
    return errorsPresenter().isAnswered() || (isOptional() && wasRecentlyUpdatedInThisProgram());
  }

  /**
   * Return true if this question is required but was left unanswered while filling out the current
   * program.
   */
  public boolean isRequiredButWasUnansweredInCurrentProgram() {
    return !isOptional() && !errorsPresenter().isAnswered() && wasRecentlyUpdatedInThisProgram();
  }

  /** Returns true if this question was most recently updated in this program. */
  private boolean wasRecentlyUpdatedInThisProgram() {
    return getUpdatedInProgramMetadata().stream()
        .anyMatch(pid -> pid.equals(programQuestionDefinition.getProgramDefinitionId()));
  }

  /**
   * Get the question text localized to the applicant's preferred locale, contextualized with {@link
   * RepeatedEntity}.
   */
  public String getQuestionText() {
    String text =
        getQuestionDefinition().getQuestionText().getOrDefault(applicantData.preferredLocale());
    return repeatedEntity.map(r -> r.contextualize(text)).orElse(text);
  }

  /**
   * Get the question help text localized to the applicant's preferred locale, contextualized with
   * {@link RepeatedEntity}.
   */
  public String getQuestionHelpText() {
    String helpText =
        getQuestionDefinition().getQuestionHelpText().getOrDefault(applicantData.preferredLocale());
    return repeatedEntity.map(r -> r.contextualize(helpText)).orElse(helpText);
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
    return getQuestionDefinition()
        .getContextualizedPath(repeatedEntity, ApplicantData.APPLICANT_PATH);
  }

  /**
   * Returns the map of contextualized paths to scalars and their {@link ScalarType}s used by this
   * question. This includes metadata paths.
   *
   * <p>This should not be used for {@link QuestionType#ENUMERATOR} questions.
   */
  public ImmutableMap<Path, ScalarType> getContextualizedScalars() {
    try {
      return ImmutableSet.<Scalar>builder()
          .addAll(Scalar.getScalars(getType()))
          .addAll(Scalar.getMetadataScalars())
          .build()
          .stream()
          .collect(
              ImmutableMap.toImmutableMap(
                  scalar -> getContextualizedPath().join(scalar), scalar -> scalar.toScalarType()));
    } catch (InvalidQuestionTypeException | UnsupportedQuestionTypeException e) {
      throw new RuntimeException(e);
    }
  }

  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    return errorsPresenter().getQuestionErrors();
  }

  public boolean hasErrors() {
    return errorsPresenter().hasQuestionErrors() || errorsPresenter().hasTypeSpecificErrors();
  }

  public Optional<Long> getUpdatedInProgramMetadata() {
    return getMetadata(Scalar.PROGRAM_UPDATED_IN);
  }

  public Optional<Long> getLastUpdatedTimeMetadata() {
    return getMetadata(Scalar.UPDATED_AT);
  }

  private Optional<Long> getMetadata(Scalar metadataScalar) {
    Path contextualizedMetadataPath = getMetadataPath(metadataScalar);
    return applicantData.readLong(contextualizedMetadataPath);
  }

  private Path getMetadataPath(Scalar metadataScalar) {
    // For enumerator questions, rely on the metadata at the first repeated entity if it exists.
    // If it doesn't exist, check for metadata stored when there are no repeated entities.
    if (getQuestionDefinition().isEnumerator()) {
      Path firstEntity = getContextualizedPath().atIndex(0);
      return applicantData.hasPath(firstEntity)
          ? firstEntity.join(metadataScalar)
          : getContextualizedPath().withoutArrayReference().join(metadataScalar);
    }
    return getContextualizedPath().join(metadataScalar);
  }

  public AddressQuestion createAddressQuestion() {
    return new AddressQuestion(this);
  }

  public DateQuestion createDateQuestion() {
    return new DateQuestion(this);
  }

  public EmailQuestion createEmailQuestion() {
    return new EmailQuestion(this);
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

  public StaticContentQuestion createStaticContentQuestion() {
    return new StaticContentQuestion(this);
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
      case EMAIL:
        return createEmailQuestion();
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
      case STATIC:
        return createStaticContentQuestion();
      default:
        throw new RuntimeException("Unrecognized question type: " + getType());
    }
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof ApplicantQuestion) {
      ApplicantQuestion that = (ApplicantQuestion) object;
      return this.getQuestionDefinition().equals(that.getQuestionDefinition())
          && this.applicantData.equals(that.applicantData);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getQuestionDefinition(), applicantData);
  }
}
