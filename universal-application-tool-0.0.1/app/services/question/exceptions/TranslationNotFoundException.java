package services.question.exceptions;

import java.util.Locale;
import services.Path;

public class TranslationNotFoundException extends Exception {
  public TranslationNotFoundException(Path path, Locale locale) {
    super(
        String.format(
            "Translation not found for Question at path: %1s\n\tLocale: %2s", path, locale));
  }
}
