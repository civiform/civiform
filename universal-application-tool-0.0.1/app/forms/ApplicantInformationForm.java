package forms;

import java.util.Locale;
import services.LocalizedStrings;

public class ApplicantInformationForm {
  private Locale locale;

  public ApplicantInformationForm() {
    locale = LocalizedStrings.DEFAULT_LOCALE;
  }

  public Locale getLocale() {
    return this.locale;
  }

  public void setLocale(String languageTag) {
    this.locale = Locale.forLanguageTag(languageTag);
  }
}
