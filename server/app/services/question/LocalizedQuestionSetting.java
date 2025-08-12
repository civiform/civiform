package services.question;

import com.google.auto.value.AutoValue;
import java.util.Locale;
import views.components.TextFormatter;

/**
 * Represents a question setting in a {@link services.question.types.QuestionDefinition} for
 * question types that support Question Settings, localized to a specific locale.
 */
@AutoValue
public abstract class LocalizedQuestionSetting {

  /** Create a LocalizedQuestionSetting. */
  public static LocalizedQuestionSetting create(
      String settingKey, SettingType settingType, String settingDisplayName, Locale locale) {
    return new AutoValue_LocalizedQuestionSetting(
        settingKey, settingType, settingDisplayName, locale);
  }

  /** The key used for the setting. */
  public abstract String settingKey();

  /** Identifier indicating how this setting will be used. */
  public abstract SettingType settingType();

  /** The text string to display to the user. */
  public abstract String settingDisplayName();

  /** Sanitized HTML for the option that processes Markdown. */
  public String formattedSettingDisplayName(String ariaLabelForNewTabs) {
    return TextFormatter.formatTextToSanitizedHTML(
        settingDisplayName(), false, false, ariaLabelForNewTabs);
  }

  /** The locale this option is localized to. */
  public abstract Locale locale();
}
