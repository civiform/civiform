package services.applicant;

import com.google.auto.value.AutoValue;

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

  /** The program id that this is currently in the context of. */
  public abstract Long programId();

  /** The block id for where this question resides within the current program. */
  public abstract String blockId();

  /** The question text */
  public abstract String questionText();

  /** The applicant's response to the question. */
  public abstract String answerText();

  /** The timestamp of when the answer was saved. */
  public abstract Long timestamp();

  /** Whether the question was answered for another program. */
  public abstract boolean isPreviousResponse();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setProgramId(Long programId);

    public abstract Builder setBlockId(String blockId);

    public abstract Builder setQuestionText(String questionText);

    public abstract Builder setAnswerText(String answerText);

    public abstract Builder setTimestamp(Long timestamp);

    public abstract Builder setIsPreviousResponse(boolean isPreviousResponse);

    public abstract AnswerData build();
  }
}
