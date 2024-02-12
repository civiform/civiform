package views.questiontypes;

import com.google.auto.value.AutoValue;
import java.util.Optional;
import play.i18n.Messages;
import services.cloud.StorageUploadRequest;

/** Contains necessary parameters for an applicant question renderer to render a question. */
@AutoValue
public abstract class ApplicantQuestionRendererParams {

  /**
   * Indicates how validation errors should be rendered (or not rendered) when displaying the
   * question.
   */
  public enum ErrorDisplayMode {
    /**
     * Validation errors aren't displayed. Typically used when displaying the question to the
     * applicant prior to an attempt to submit.
     */
    HIDE_ERRORS,
    /**
     * Validation errors are displayed. Typically used when displaying a question in response to the
     * applicant submitting a form.
     */
    DISPLAY_ERRORS,
    /**
     * Validation errors are displayed, *and* a modal is displayed on top of the form asking the
     * applicant to either (1) correct the errors or (2) discard their answers and continue to the
     * Review page. Typically used when the applicant has clicked Review but their answers didn't
     * pass validation.
     */
    DISPLAY_ERRORS_WITH_MODAL_REVIEW;

    /**
     * Returns true if the given mode indicates that validation errors should be rendered when
     * displaying the form and false otherwise.
     */
    public static boolean shouldShowErrors(ErrorDisplayMode mode) {
      return mode == DISPLAY_ERRORS || mode == DISPLAY_ERRORS_WITH_MODAL_REVIEW;
    }
  }

  public static Builder builder() {
    return new AutoValue_ApplicantQuestionRendererParams.Builder()
        .setAutofocus(AutoFocusTarget.NONE)
        .setErrorDisplayMode(ErrorDisplayMode.DISPLAY_ERRORS);
  }

  public abstract Messages messages();

  public abstract Optional<StorageUploadRequest> signedFileUploadRequest();

  public abstract ErrorDisplayMode errorDisplayMode();

  public abstract AutoFocusTarget autofocus();

  /**
   * True if autofocus is FIRST_FIELD, meaning the first field of the first question on the page
   * should have the autofocus attribute.
   */
  public boolean autofocusFirstField() {
    return AutoFocusTarget.FIRST_FIELD.equals(autofocus());
  }

  /**
   * True if autofocus is FIRST_ERROR, meaning the first field on the page that has an validation
   * error message should have the autofocus attribute.
   */
  public boolean autofocusFirstError() {
    return AutoFocusTarget.FIRST_ERROR.equals(autofocus());
  }

  /** True if a question that only has a single field should autofocus it. */
  public boolean autofocusSingleField() {
    return AutoFocusTarget.FIRST_ERROR.equals(autofocus())
        || AutoFocusTarget.FIRST_FIELD.equals(autofocus());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setMessages(Messages messages);

    public abstract Builder setSignedFileUploadRequest(
        StorageUploadRequest signedFileUploadRequest);

    public abstract Builder setErrorDisplayMode(ErrorDisplayMode errorDisplayMode);

    public abstract Builder setAutofocus(AutoFocusTarget autofocus);

    public abstract ApplicantQuestionRendererParams build();
  }

  /** Specifies autofocus logic. */
  public enum AutoFocusTarget {
    // Question should not autofocus
    NONE,
    // Autofocus the first field on the page that has a validation error message
    FIRST_ERROR,
    // Autofocus first field in the question on the page
    FIRST_FIELD;
  }
}
