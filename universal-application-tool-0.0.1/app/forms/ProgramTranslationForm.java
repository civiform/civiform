package forms;

import java.util.Locale;

public class ProgramTranslationForm {

  private Locale locale;
  private String displayName;
  private String displayDescription;

  public ProgramTranslationForm() {
    this.locale = Locale.US;
    this.displayName = "";
    this.displayDescription = "";
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
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
