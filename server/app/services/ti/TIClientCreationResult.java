package services.ti;

import forms.AddApplicantToTrustedIntermediaryGroupForm;
import java.util.Optional;
import play.data.Form;

/**
 * Holds state relevant to the result of attempting to create a Client for TrustedIntermediary.
 *
 * <p>If the creation attempt was successful, the response is a successful object with an empty
 * form.
 *
 * <p>If the creation attempt was not successful, the result contains the same form object with the
 * form validation errors.
 */
public final class TIClientCreationResult {

  private final Optional<Form<AddApplicantToTrustedIntermediaryGroupForm>> form;

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
