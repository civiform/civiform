package views.questiontypes;

import com.google.auto.value.AutoValue;
import java.util.Optional;
import play.i18n.Messages;
import services.cloud.StorageUploadRequest;

/** Contains necessary parameters for an applicant question renderer to render a question. */
@AutoValue
public abstract class ApplicantQuestionRendererParams {

  public static Builder builder() {
    return new AutoValue_ApplicantQuestionRendererParams.Builder().setIsSample(false);
  }

  public static ApplicantQuestionRendererParams sample(Messages messages) {
    return builder().setIsSample(true).setDisplayErrors(false).setMessages(messages).build();
  }

  public abstract boolean isSample();

  public abstract Messages messages();

  public abstract Optional<StorageUploadRequest> signedFileUploadRequest();

  public abstract boolean displayErrors();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setIsSample(boolean isSample);

    public abstract Builder setMessages(Messages messages);

    public abstract Builder setSignedFileUploadRequest(
        StorageUploadRequest signedFileUploadRequest);

    public abstract Builder setDisplayErrors(boolean displayErrors);

    public abstract ApplicantQuestionRendererParams build();
  }
}
