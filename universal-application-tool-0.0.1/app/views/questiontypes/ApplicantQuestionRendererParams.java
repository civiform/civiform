package views.questiontypes;

import com.google.auto.value.AutoValue;
import play.i18n.Messages;

@AutoValue
public abstract class ApplicantQuestionRendererParams {
  public static final long FAKE_APPLICANT_ID = 9999L;
  public static final long FAKE_PROGRAM_ID = 9999L;

  public static Builder builder() {
    return new AutoValue_ApplicantQuestionRendererParams.Builder();
  }

  public static ApplicantQuestionRendererParams sample(Messages messages) {
    return builder()
        .setApplicantId(FAKE_APPLICANT_ID)
        .setProgramId(FAKE_PROGRAM_ID)
        .setMessages(messages)
        .build();
  }

  public abstract Messages messages();

  public abstract long applicantId();

  public abstract long programId();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setMessages(Messages messages);

    public abstract Builder setApplicantId(long applicantId);

    public abstract Builder setProgramId(long programId);

    public abstract ApplicantQuestionRendererParams build();
  }
}
