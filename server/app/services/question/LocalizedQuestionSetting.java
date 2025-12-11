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
      String settingKey,
      SettingType settingType,
      String settingDisplayName,
      String settingValue,
      String settingText,
      Locale locale) {
    return new AutoValue_LocalizedQuestionSetting(
        settingKey, settingType, settingDisplayName, settingValue, settingText, locale);
  }

  /**
   * The key identifying this setting that is provided by CiviForm Admins during question creation.
   * For map questions, this maps to a key in the GeoJSON feature's properties object.
   *
   * @return a string (e.g. 'name' or 'address')
   */
  public abstract String settingKey();

  /** Identifier indicating how this setting will be used. */
  public abstract SettingType settingType();

  /** The text string to display to the user. */
  public abstract String settingDisplayName();

  /**
   * The value for this setting. For map questions, this maps to a specific value in the GeoJSON
   * feature's properties object.
   */
  public abstract String settingValue();

  /** Additional text to display to the user for this setting. */
  public abstract String settingText();

  /** Sanitized HTML for the option that processes Markdown. */
  public String formattedSettingDisplayName(String ariaLabelForNewTabs) {
    return TextFormatter.formatTextToSanitizedHTML(
        settingDisplayName(), false, false, ariaLabelForNewTabs);
  }

  /** The locale this option is localized to. */
  public abstract Locale locale();
}
