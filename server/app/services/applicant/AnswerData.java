package services.applicant;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.Path;
import services.applicant.question.AbstractQuestion;
import services.applicant.question.ApplicantQuestion;
import services.question.types.QuestionDefinition;

/**
 * This class is a summary of the data for a specific applicant and question. It includes the
 * question and answer text as well as information about when the question was answered.
 */
@AutoValue
public abstract class AnswerData {
  /** When no timestamp is available, the value is set to -1. */
  public static final Long TIMESTAMP_NOT_SET = -1L;

  public static Builder builder() {
    return new AutoValue_AnswerData.Builder();
  }

  /** The {@link models.ProgramModel} id that this is currently in the context of. */
  public abstract long programId();

  /** The {@link Block} id for where this question resides within the current program. */
  public abstract String blockId();

  /** The {@link Path} for this answer in the applicant's {@code ApplicantData}. */
  public abstract Path contextualizedPath();

  /** The {@link QuestionDefinition} for this question. */
  public abstract QuestionDefinition questionDefinition();

  /** The {@link ApplicantQuestion} for this question. */
  public abstract ApplicantQuestion applicantQuestion();

  /** The repeated entity if this is an answer to a repeated question. Otherwise, empty. */
  public abstract Optional<RepeatedEntity> repeatedEntity();

  /**
   * The index of the {@link models.QuestionModel} this is an answer for in the block it appeared
   * in.
   */
  public abstract int questionIndex();

  /** The localized question text */
  public abstract String questionText();

  /**
   * The localized question text that should be used for screen readers. We add a styled asterisk,
   * which should be read out to screen readers.
   */
  public abstract String questionTextForScreenReader();

  /** True if this answer represents an answer, or false for a skipped question. */
  public abstract boolean isAnswered();

  /**
   * True if this answer meets eligibility criteria for the block it is in. Unanswered eligibility
   * questions will have a false value.
   */
  public abstract boolean isEligible();

  /**
   * True if the program's eligibility is set to gating. If false, eligibility is NOT gating tags
   * that note that the applicant may not qualify should also be hidden.
   */
  public abstract boolean eligibilityIsGating();

  /** The applicant's response to the question. */
  public abstract String answerText();

  /** The identifier of the applicant's uploaded file after its UTF-8 encoded if applicable */
  public abstract Optional<String> encodedFileKey();

  /**
   * Identifiers for the applicant's uploaded files, UTF-8 encoded.
   *
   * <p>Only set for questions answered when the "MULTIPLE_FILE_UPLOAD_ENABLED" feature flag is
   * enabled.
   */
  public abstract ImmutableList<String> encodedFileKeys();

  /**
   * File names for the applicant's uploaded files. Will always be the same size as {@link
   * #encodedFileKeys}, with corresponding indicies.
   */
  public abstract ImmutableList<String> fileNames();

  /**
   * The original file name of the applicant's uploaded file, if applicable. For example, this is
   * needed for Azure blob storage, where the original file name is different from the file key.
   */
  public abstract Optional<String> originalFileName();

  /** The timestamp of when the answer was saved. */
  public abstract long timestamp();

  /** Whether the question was answered for another program. */
  public abstract boolean isPreviousResponse();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setProgramId(Long programId);

    public abstract Builder setBlockId(String blockId);

    public abstract Builder setContextualizedPath(Path path);

    public abstract Builder setQuestionDefinition(QuestionDefinition questionDefinition);

    public abstract Builder setApplicantQuestion(ApplicantQuestion applicantQuestion);

    public abstract Builder setRepeatedEntity(Optional<RepeatedEntity> repeatedEntity);

    public abstract Builder setQuestionIndex(int questionIndex);

    public abstract Builder setQuestionText(String questionText);

    public abstract Builder setQuestionTextForScreenReader(String questionTextForScreenReader);

    public abstract Builder setIsAnswered(boolean isAnswered);

    public abstract Builder setIsEligible(boolean isEligible);

    public abstract Builder setEligibilityIsGating(boolean eligibilityIsGating);

    public abstract Builder setAnswerText(String answerText);

    public abstract Builder setEncodedFileKey(Optional<String> encodedFileKey);

    public abstract Builder setOriginalFileName(Optional<String> originalFileName);

    public abstract Builder setEncodedFileKeys(ImmutableList<String> encodedFileKeys);

    public abstract Builder setFileNames(ImmutableList<String> fileNames);

    public abstract Builder setTimestamp(Long timestamp);

    public abstract Builder setIsPreviousResponse(boolean isPreviousResponse);

    public abstract AnswerData build();
  }

  /** Creates a {@link AbstractQuestion} for the given {@link AnswerData}'s type. */
  public AbstractQuestion createQuestion() {
    switch (questionDefinition().getQuestionType()) {
      case ENUMERATOR:
        return applicantQuestion().createEnumeratorQuestion();
      case STATIC:
        return applicantQuestion().createStaticContentQuestion();
      case CHECKBOX:
        return applicantQuestion().createMultiSelectQuestion();
      case CURRENCY:
        return applicantQuestion().createCurrencyQuestion();
      case NUMBER:
        return applicantQuestion().createNumberQuestion();
      case DATE:
        return applicantQuestion().createDateQuestion();
      case PHONE:
        return applicantQuestion().createPhoneQuestion();
      case NAME:
        return applicantQuestion().createNameQuestion();
      case ID:
        return applicantQuestion().createIdQuestion();
      case TEXT:
        return applicantQuestion().createTextQuestion();
      case EMAIL:
        return applicantQuestion().createEmailQuestion();
      case ADDRESS:
        return applicantQuestion().createAddressQuestion();
      case DROPDOWN:
      case RADIO_BUTTON:
        return applicantQuestion().createSingleSelectQuestion();
      case FILEUPLOAD:
        return applicantQuestion().createFileUploadQuestion();
      case NULL_QUESTION: // fallthrough intended
      default:
        throw new RuntimeException(
            String.format("Unknown QuestionType %s", questionDefinition().getQuestionType()));
    }
  }
}
