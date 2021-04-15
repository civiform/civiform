package services.question;

import com.google.auto.value.AutoValue;
import java.util.Locale;

/**
 * Represents a single option in a {@link services.question.types.MultiOptionQuestionDefinition},
 * localized to a specific locale.
 */
@AutoValue
public abstract class LocalizedQuestionOption {

  /** Create a LocalizedQuestionOption. */
  public static LocalizedQuestionOption create(long id, String optionText, Locale locale) {
    return new AutoValue_LocalizedQuestionOption(id, optionText, locale);
  }

  /** The id for this option. */
  public abstract long id();

  /** The text strings to display to the user. */
  public abstract String optionText();

  /** The locale this option is localized to. */
  public abstract Locale locale();
}
