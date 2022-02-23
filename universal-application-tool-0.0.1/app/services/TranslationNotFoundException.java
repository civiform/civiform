package services;

import java.util.Locale;

/**
 * TranslationNotFoundException is thrown when a translation cannot be found for the specified
 * locale.
 */
public class TranslationNotFoundException extends Exception {
  public TranslationNotFoundException(Locale locale) {
    super(String.format("No translation was found for locale %s", locale));
  }
}
