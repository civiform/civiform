package forms;

/** Form for updating whether a question has address correction enabled. */
public final class ProgramQuestionDefinitionAddressCorrectionEnabledForm {
  private Boolean addressCorrectionEnabled;

  public ProgramQuestionDefinitionAddressCorrectionEnabledForm() {
    addressCorrectionEnabled = false;
  }

  public Boolean getAddressCorrectionEnabled() {
    return addressCorrectionEnabled;
  }

  public void setAddressCorrectionEnabled(Boolean addressCorrectionEnabled) {
    this.addressCorrectionEnabled = addressCorrectionEnabled;
  }
}
