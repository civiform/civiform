package forms;

import java.util.Locale;
import services.LocalizedStrings;

/** Form for updating a user's information, e.g. preferred language. */
public final class ApplicantInformationForm {
  private Locale locale;
  private String redirectLink;

  public ApplicantInformationForm() {
    locale = LocalizedStrings.DEFAULT_LOCALE;
    redirectLink = "";
  }

  public Locale getLocale() {
    return this.locale;
  }

  public String getRedirectLink() {
    return this.redirectLink;
  }

  public void setLocale(String languageTag) {
    this.locale = Locale.forLanguageTag(languageTag);
  }

  public void setRedirectLink(String redirectLink) {
    this.redirectLink = redirectLink;
  }
}
