package services.ti;

import forms.TiClientInfoForm;
import java.util.Optional;
import models.ApplicantModel;
import play.data.Form;

public class TIClientFormWithOptionalApplicantData {
  private Form<TiClientInfoForm> form;

  public Form<TiClientInfoForm> getForm() {
    return form;
  }

  public void setForm(Form<TiClientInfoForm> form) {
    this.form = form;
  }

  public Optional<ApplicantModel> getOptionalApplicantModel() {
    return optionalApplicantModel;
  }

  public void setOptionalApplicantModel(Optional<ApplicantModel> optionalApplicantModel) {
    this.optionalApplicantModel = optionalApplicantModel;
  }

  private Optional<ApplicantModel> optionalApplicantModel;

  public TIClientFormWithOptionalApplicantData(
      Form<TiClientInfoForm> form, Optional<ApplicantModel> optionalApplicantModel) {
    this.optionalApplicantModel = optionalApplicantModel;
    this.form = form;
  }
}
