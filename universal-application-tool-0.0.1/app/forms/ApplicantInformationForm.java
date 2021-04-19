package forms;

import java.util.Locale;

public class ApplicantInformationForm {
  private Locale locale;

  public ApplicantInformationForm() {
    // The default language for CiviForm is US English.
    locale = Locale.US;
  }

  public Locale getLocale() {
    return this.locale;
  }

  public void setLocale(String languageTag) {
    this.locale = Locale.forLanguageTag(languageTag);
  }
}
