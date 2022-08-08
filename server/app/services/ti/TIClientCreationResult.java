package services.ti;

import forms.AddApplicantToTrustedIntermediaryGroupForm;
import java.util.Optional;
import play.data.Form;
import play.mvc.StatusHeader;

public class TIClientCreationResult {

  private final Optional<Form<AddApplicantToTrustedIntermediaryGroupForm>> form;
  private final Optional<StatusHeader> statusHeader;
  // private final Optional<String> errorMessage;

  public TIClientCreationResult(
      Optional<Form<AddApplicantToTrustedIntermediaryGroupForm>> form,
      Optional<StatusHeader> statusHeader) {
    this.form = form;
    // this.errorMessage=errorMessage
    this.statusHeader = statusHeader;
  }

  /** Constructs an instance in the case of success. */
  public static TIClientCreationResult success() {
    return new TIClientCreationResult(
        /* form= */ Optional.empty(), /* statusHeader= */ Optional.empty());
  }

  /** Constructs an instance in the case of failure. */
  public static TIClientCreationResult failure(
      Form<AddApplicantToTrustedIntermediaryGroupForm> form, Optional<StatusHeader> statusHeader) {
    return new TIClientCreationResult(Optional.of(form), statusHeader);
  }

  public boolean isSuccessful() {
    return form.isEmpty() && statusHeader.isEmpty();
  }

  public Optional<StatusHeader> getStatusHeader() {
    return statusHeader;
  }

  public Optional<Form<AddApplicantToTrustedIntermediaryGroupForm>> getForm() {
    return form;
  }
}
