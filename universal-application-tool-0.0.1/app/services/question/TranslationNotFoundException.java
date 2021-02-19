package services.question;

import java.util.Locale;

public class TranslationNotFoundException extends Exception {
  public TranslationNotFoundException(String pathName, Locale locale) {
    super(
        String.format(
            "Translation not found for Question at path: %1s\n\tLocale: %2s", pathName, locale));
  }
}
