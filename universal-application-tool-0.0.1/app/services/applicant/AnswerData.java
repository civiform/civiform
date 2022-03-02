package services.applicant;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import services.Path;
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

  /** The {@link models.Program} id that this is currently in the context of. */
  public abstract Long programId();

  /** The {@link Block} id for where this question resides within the current program. */
  public abstract String blockId();

  /** The {@link Path} for this answer in the applicant's {@code ApplicantData}. */
  public abstract Path contextualizedPath();

  /** The {@link models.Question} ID this is an answer for. */
  public abstract QuestionDefinition questionDefinition();

  /** The repeated entity if this is an answer to a repeated question. Otherwise, empty. */
  public abstract Optional<RepeatedEntity> repeatedEntity();

  /** The index of the {@link models.Question} this is an answer for in the block it appeared in. */
  public abstract int questionIndex();

  /** The localized question text */
  public abstract String questionText();

  /** True if this answer represents an answer, or false for a skipped question. */
  public abstract boolean isAnswered();

  /** The applicant's response to the question. */
  public abstract String answerText();

  /** The identifier of the applicant's uploaded file if applicable. */
  public abstract Optional<String> fileKey();

  /**
   * The original file name of the applicant's uploaded file, if applicable. This is only needed for
   * Azure blob storage, where the original file name is different from the file key.
   */
  public abstract Optional<String> originalFileName();

  /** The timestamp of when the answer was saved. */
  public abstract Long timestamp();

  /** Whether the question was answered for another program. */
  public abstract boolean isPreviousResponse();

  /**
   * Paths and their answers for each scalar (in {@link services.LocalizedStrings#DEFAULT_LOCALE}
   * for {@link services.question.LocalizedQuestionOption}s based answers) to present to admins.
   */
  public abstract ImmutableMap<Path, String> scalarAnswersInDefaultLocale();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setProgramId(Long programId);

    public abstract Builder setBlockId(String blockId);

    public abstract Builder setContextualizedPath(Path path);

    public abstract Builder setQuestionDefinition(QuestionDefinition questionDefinition);

    public abstract Builder setRepeatedEntity(Optional<RepeatedEntity> repeatedEntity);

    public abstract Builder setQuestionIndex(int questionIndex);

    public abstract Builder setQuestionText(String questionText);

    public abstract Builder setIsAnswered(boolean isAnswered);

    public abstract Builder setAnswerText(String answerText);

    public abstract Builder setFileKey(Optional<String> fileKey);

    public abstract Builder setOriginalFileName(Optional<String> originalFileName);

    public abstract Builder setTimestamp(Long timestamp);

    public abstract Builder setIsPreviousResponse(boolean isPreviousResponse);

    public abstract Builder setScalarAnswersInDefaultLocale(ImmutableMap<Path, String> answers);

    public abstract AnswerData build();
  }
}
