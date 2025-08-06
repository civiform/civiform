package services.question;

import com.google.auto.value.AutoValue;
import java.util.Locale;
import views.components.TextFormatter;

/**
 * Represents a single option in a {@link services.question.types.MapQuestionDefinition}, localized
 * to a specific locale.
 */
@AutoValue
public abstract class LocalizedQuestionSetting {

  /** Create a LocalizedQuestionSetting. */
  public static LocalizedQuestionSetting create(String settingDisplayName, Locale locale) {
    return new AutoValue_LocalizedQuestionSetting(settingDisplayName, locale);
  }

  /** The text strings to display to the user. */
  public abstract String settingDisplayName();

  /** Sanitized HTML for the option that processes Markdown. */
  public String formattedSettingDisplayName(String ariaLabelForNewTabs) {
    return TextFormatter.formatTextToSanitizedHTML(
        settingDisplayName(), false, false, ariaLabelForNewTabs);
  }

  /** The locale this option is localized to. */
  public abstract Locale locale();
}
