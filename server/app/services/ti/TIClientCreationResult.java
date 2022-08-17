package services.ti;

import forms.AddApplicantToTrustedIntermediaryGroupForm;
import java.util.Optional;
import play.data.Form;

public class TIClientCreationResult {

  private final Optional<Form<AddApplicantToTrustedIntermediaryGroupForm>> form;
  // private final Optional<String> errorMessage;

  public TIClientCreationResult(Optional<Form<AddApplicantToTrustedIntermediaryGroupForm>> form) {
    this.form = form;
  }

  /** Constructs an instance in the case of success. */
  public static TIClientCreationResult success() {
    return new TIClientCreationResult(/* form= */ Optional.empty());
  }

  /** Constructs an instance in the case of failure. */
  public static TIClientCreationResult failure(
      Form<AddApplicantToTrustedIntermediaryGroupForm> form) {
    return new TIClientCreationResult(Optional.of(form));
  }

  public boolean isSuccessful() {
    return form.isEmpty();
  }

  public Optional<Form<AddApplicantToTrustedIntermediaryGroupForm>> getForm() {
    return form;
  }
}
