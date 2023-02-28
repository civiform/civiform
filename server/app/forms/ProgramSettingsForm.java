package forms;

/** Form for updating program-level settings. */
public final class ProgramSettingsForm {
  private Boolean eligibilityIsGating;

  public ProgramSettingsForm() {
    eligibilityIsGating = true;
  }

  public Boolean getEligibilityIsGating() {
    return eligibilityIsGating;
  }

  public void setEligibilityIsGating(Boolean eligibilityIsGating) {
    this.eligibilityIsGating = eligibilityIsGating;
  }
}
