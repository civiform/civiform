package services.question;

import com.google.auto.value.AutoValue;
import java.util.Locale;
import views.components.TextFormatter;

/**
 * Represents a single option in a {@link services.question.types.MultiOptionQuestionDefinition},
 * localized to a specific locale.
 */
@AutoValue
public abstract class LocalizedQuestionOption {

  /** Create a LocalizedQuestionOption. */
  public static LocalizedQuestionOption create(
      long id, long order, String adminName, String optionText, Locale locale) {
    return new AutoValue_LocalizedQuestionOption(id, order, adminName, optionText, locale);
  }

  /** The id for this option. */
  public abstract long id();

  /** The order of the option. */
  public abstract long order();

  /** The immutable identifier for this option, used to reference it in the API and predicates. */
  public abstract String adminName();

  /** The text strings to display to the user. */
  public abstract String optionText();

  /** Sanitized HTML for the option that processes Markdown. */
  public String formattedOptionText(String ariaLabel) {
    return TextFormatter.formatTextToSanitizedHTMLWithAriaLabel(
        optionText(), false, false, ariaLabel);
  }

  /** The locale this option is localized to. */
  public abstract Locale locale();
}
