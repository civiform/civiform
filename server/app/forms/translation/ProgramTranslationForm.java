package forms.translation;

/** Form for updating translation for programs. */
public class ProgramTranslationForm {

  private String displayName;
  private String displayDescription;

  public ProgramTranslationForm() {
    this.displayName = "";
    this.displayDescription = "";
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayDescription() {
    return displayDescription;
  }

  public void setDisplayDescription(String displayDescription) {
    this.displayDescription = displayDescription;
  }
}
