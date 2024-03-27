package services.ti;

import forms.TiClientInfoForm;
import java.util.Optional;
import play.data.Form;

/**
 * Class to hold the return variables needed to render the AddNewTiClient form both on success and
 * failure.
 */
public final class AddNewApplicantReturnObject {
  private Form<TiClientInfoForm> form;

  public Form<TiClientInfoForm> getForm() {
    return form;
  }

  public void setForm(Form<TiClientInfoForm> form) {
    this.form = form;
  }

  public Optional<Long> getoptionalApplicantId() {
    return optionalApplicantId;
  }

  public void setoptionalApplicantId(Optional<Long> optionalApplicantId) {
    this.optionalApplicantId = optionalApplicantId;
  }

  private Optional<Long> optionalApplicantId;

  public AddNewApplicantReturnObject(
      Form<TiClientInfoForm> form, Optional<Long> optionalApplicantId) {
    this.optionalApplicantId = optionalApplicantId;
    this.form = form;
  }
}
