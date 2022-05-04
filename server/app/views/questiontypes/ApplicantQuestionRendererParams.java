package views.questiontypes;

import com.google.auto.value.AutoValue;
import java.util.Optional;
import play.i18n.Messages;
import services.cloud.StorageUploadRequest;

/** Contains necessary parameters for an applicant question renderer to render a question. */
@AutoValue
public abstract class ApplicantQuestionRendererParams {

  /** Indicates whether validation errors should be rendered when displaying the question. */
  public enum ErrorDisplayMode {
    /**
     * Validation errors aren't displayed. Typically used when displaying the question to the
     * applicant prior to an attempt to submit.
     */
    HIDE_ERRORS,
    /**
     * Validation errors are displayed. Typically used when displaying a question in response to the
     * applicant attempting a submit.
     */
    DISPLAY_ERRORS
  }

  public static Builder builder() {
    return new AutoValue_ApplicantQuestionRendererParams.Builder().setIsSample(false);
  }

  public static ApplicantQuestionRendererParams sample(Messages messages) {
    return builder()
        .setIsSample(true)
        .setErrorDisplayMode(ErrorDisplayMode.HIDE_ERRORS)
        .setMessages(messages)
        .build();
  }

  public abstract boolean isSample();

  public abstract Messages messages();

  public abstract Optional<StorageUploadRequest> signedFileUploadRequest();

  public abstract ErrorDisplayMode errorDisplayMode();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setIsSample(boolean isSample);

    public abstract Builder setMessages(Messages messages);

    public abstract Builder setSignedFileUploadRequest(
        StorageUploadRequest signedFileUploadRequest);

    public abstract Builder setErrorDisplayMode(ErrorDisplayMode errorDisplayMode);

    public abstract ApplicantQuestionRendererParams build();
  }
}
