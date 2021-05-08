package services;

import java.util.Locale;

public class TranslationNotFoundException extends Exception {
  public TranslationNotFoundException(Locale locale) {
    super(String.format("No translation was found for locale %s", locale));
  }
}
