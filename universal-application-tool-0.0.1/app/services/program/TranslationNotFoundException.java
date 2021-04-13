package services.program;

import java.util.Locale;

public class TranslationNotFoundException extends Exception {
  public TranslationNotFoundException(long programId, Locale locale) {
    super(String.format("No translation was found for locale %s in program %d", locale, programId));
  }
}
