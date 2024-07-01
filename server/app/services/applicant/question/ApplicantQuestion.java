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
import services.program.ProgramQuestionDefinition;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.question.types.ScalarType;
import views.components.TextFormatter;

/**
 * Represents a question in the context of a specific applicant. Other type-specific classes (e.g.
 * {@link NameQuestion}) use this class's data to represent a specific question type. These other
 * classes provide access to the applicant's answer for the question. They can also implement
 * server-side validation logic.
 */
public final class ApplicantQuestion {

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

  public ApplicantData getApplicantData() {
    return this.applicantData;
  }

  public QuestionDefinition getQuestionDefinition() {
    return this.programQuestionDefinition.getQuestionDefinition();
  }

  public QuestionType getType() {
    return getQuestionDefinition().getQuestionType();
  }

  public long getProgramId() {
    return programQuestionDefinition.getProgramDefinitionId();
  }

  public boolean isOptional() {
    return programQuestionDefinition.optional();
  }

  /**
   * Return true if this question is answered or a skipped optional question in the program
   * specified. Questions can only be skipped and left unanswered if they are optional.
   *
   * <p>For every applicant question, there are three possible states:
   *
   * <ol>
   *   <li>unvisited - the applicant has not seen this question yet
   *   <li>skipped - the applicant has visited the question but chose to leave it unanswered. Only
   *       optional questions can be skipped and left unanswered.
   *   <li>answered - the applicant has visited and provided an answer to the question
   * </ol>
   *
   * For skipped optional questions, we care in which program it was skipped. For the program which
   * the optional question was skipped and left unanswered, it counts as "completed" for that
   * program.
   *
   * <p>Static questions evaluate as answered since they are not awaiting a response.
   *
   * @return true if this question is answered or it is an optional question that was skipped in the
   *     program specified.
   */
  public boolean isAnsweredOrSkippedOptionalInProgram() {
    return isAnswered() || (isOptional() && wasRecentlyUpdatedInThisProgram());
  }

  /**
   * Return true if this question is required but was skipped and left unanswered while filling out
   * the current program.
   */
  public boolean isRequiredButWasSkippedInCurrentProgram() {
    return !isOptional() && !isAnswered() && wasRecentlyUpdatedInThisProgram();
  }

  public boolean isAnswered() {
    return getQuestion().isAnswered();
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

  /** Sanitized HTML for the question text that processes Markdown. */
  public String getFormattedQuestionText() {
    return TextFormatter.formatTextToSanitizedHTML(
        getQuestionText(),
        /* preserveEmptyLines= */ true,
        /* addRequiredIndicator= */ !(isOptional() || getType().isMultiInputType()));
  }

    /** Sanitized HTML for the question text that processes Markdown, but in a context where we don't want to show required asterisks. */
    public String getFormattedQuestionTextWithoutRequiredAsterisk() {
      return TextFormatter.formatTextToSanitizedHTML(
          getQuestionText(),
          /* preserveEmptyLines= */ true,
          /* addRequiredIndicator= */ false);
    }

  /**
   * Returns the question text that should be used for screen readers.
   *
   * <p>We add a styled asterisk, which should be read out to screen readers.
   */
  public String getQuestionTextForScreenReader() {
    return isOptional() ? getQuestionText() : getQuestionText() + " *";
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

  /** Sanitized HTML for the question help text that processes Markdown. */
  public String getFormattedQuestionHelpText() {
    return TextFormatter.formatTextToSanitizedHTML(
        getQuestionHelpText(), /* preserveEmptyLines= */ true, /* addRequiredIndicator= */ false);
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
                  scalar -> getContextualizedPath().join(scalar), Scalar::toScalarType));
    } catch (InvalidQuestionTypeException | UnsupportedQuestionTypeException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean hasErrors() {
    return !getQuestion().getValidationErrors().isEmpty();
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

  public boolean isAddressCorrectionEnabled() {
    return programQuestionDefinition.addressCorrectionEnabled();
  }

  public CurrencyQuestion createCurrencyQuestion() {
    return new CurrencyQuestion(this);
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

  public boolean isFileUploadQuestion() {
    return getType().equals(QuestionType.FILEUPLOAD);
  }

  public IdQuestion createIdQuestion() {
    return new IdQuestion(this);
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

  public PhoneQuestion createPhoneQuestion() {
    return new PhoneQuestion(this);
  }

  public StaticContentQuestion createStaticContentQuestion() {
    return new StaticContentQuestion(this);
  }

  public TextQuestion createTextQuestion() {
    return new TextQuestion(this);
  }

  public Question getQuestion() {
    switch (getType()) {
      case ADDRESS:
        return createAddressQuestion();
      case CHECKBOX:
        return createMultiSelectQuestion();
      case CURRENCY:
        return createCurrencyQuestion();
      case DATE:
        return createDateQuestion();
      case EMAIL:
        return createEmailQuestion();
      case FILEUPLOAD:
        return createFileUploadQuestion();
      case ID:
        return createIdQuestion();
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
      case PHONE:
        return createPhoneQuestion();
      case NULL_QUESTION:
        throw new IllegalStateException(
            String.format(
                "Question type %s should not be rendered. Question ID: %s. Active program question"
                    + " definition is possibly pointing to an old question ID",
                getType(), getQuestionDefinition().getId()));
      default:
        throw new RuntimeException("Unrecognized question type: " + getType());
    }
  }

  @Override
  public String toString() {
    // This will print the identity since there's no useful string representation of a question.
    // Since the class is marked final and consumed in an AutoValue class, a warning will be
    // emitted since AutoValue automatically generates a toString call for each member of the
    // class.
    return super.toString();
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
