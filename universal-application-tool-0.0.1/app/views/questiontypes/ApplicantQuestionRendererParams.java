package views.questiontypes;

import com.google.auto.value.AutoValue;
import java.util.Optional;
import play.i18n.Messages;
import repository.SignedS3UploadRequest;

@AutoValue
public abstract class ApplicantQuestionRendererParams {
  public static final long FAKE_APPLICANT_ID = 9999L;
  public static final long FAKE_PROGRAM_ID = 9999L;
  public static final String FAKE_BLOCK_ID = "fake-block";

  public static Builder builder() {
    return new AutoValue_ApplicantQuestionRendererParams.Builder().setIsSample(false);
  }

  public static ApplicantQuestionRendererParams sample(Messages messages) {
    return builder()
        .setIsSample(true)
        .setMessages(messages)
        .setApplicantId(FAKE_APPLICANT_ID)
        .setProgramId(FAKE_PROGRAM_ID)
        .setBlockId(FAKE_BLOCK_ID)
        .build();
  }

  public abstract boolean isSample();

  public abstract Messages messages();

  public abstract long applicantId();

  public abstract long programId();

  public abstract String blockId();

  public abstract Optional<SignedS3UploadRequest> signedFileUploadRequest();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setIsSample(boolean isSample);

    public abstract Builder setMessages(Messages messages);

    public abstract Builder setApplicantId(long applicantId);

    public abstract Builder setProgramId(long programId);

    public abstract Builder setBlockId(String blockId);

    public abstract Builder setSignedFileUploadRequest(
        SignedS3UploadRequest signedFileUploadRequest);

    public abstract ApplicantQuestionRendererParams build();
  }
}
