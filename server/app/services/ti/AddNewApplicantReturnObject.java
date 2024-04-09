package services.ti;

import forms.TiClientInfoForm;
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

  public Long getApplicantId() {
    return applicantId;
  }

  public void setApplicantId(Long applicantId) {
    this.applicantId = applicantId;
  }

  private Long applicantId;

  public AddNewApplicantReturnObject(Form<TiClientInfoForm> form, Long applicantId) {
    this.applicantId = applicantId;
    this.form = form;
  }

  public AddNewApplicantReturnObject(Form<TiClientInfoForm> form) {
    this.applicantId = null;
    this.form = form;
  }
}
