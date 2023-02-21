package forms;

/** Form for updating program-level settings. */
public final class ProgramSettingsForm {
  private Boolean isEligibilityGating;

  public ProgramSettingsForm() {
    isEligibilityGating = true;
  }

  public Boolean getIsEligibilityGating() {
    return isEligibilityGating;
  }

  public void setIsEligibilityGating(Boolean isEligibilityGating) {
    this.isEligibilityGating = isEligibilityGating;
  }
}
