package services.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.Locale;
import java.util.Optional;
import services.LocalizedStrings;
import services.TranslationNotFoundException;

/**
 * Represents a question setting in a {@link services.question.types.QuestionDefinition} for
 * question types that support Question Settings.
 */
@JsonDeserialize(builder = AutoValue_QuestionSetting.Builder.class)
@AutoValue
public abstract class QuestionSetting {

  /**
   * The key identifying this setting within the question, provided by CiviForm Admins during
   * question creation. For map questions, this maps to a GeoJSON property key (e.g., 'name',
   * 'address', 'fullDayCare').
   *
   * @return a string representing the setting key
   */
  @JsonProperty("settingKey")
  public abstract String settingKey();

  /** Identifier indicating how this setting will be used, stored as string. */
  @JsonProperty("settingTypeString")
  public abstract String settingTypeString();

  /** Get the setting type as a SettingType enum. */
  public SettingType settingType() {
    return MapSettingType.valueOf(settingTypeString());
  }

  /** The text strings to display to the user, keyed by locale. Only required for some settings. */
  @JsonProperty("localizedSettingDisplayName")
  public abstract Optional<LocalizedStrings> localizedSettingDisplayName();

  /**
   * The value for this setting. For map questions, this maps to a specific GeoJSON property value
   * (e.g., 'ccapASA' for a tag filter). Optional - only used for certain setting types like tags.
   *
   * @return an optional string representing the setting value
   */
  @JsonProperty("settingValue")
  public abstract Optional<String> settingValue();

  /**
   * Additional text to display to the user for this setting, keyed by locale. Optional - only used
   * for certain setting types.
   */
  @JsonProperty("localizedSettingText")
  public abstract Optional<LocalizedStrings> localizedSettingText();

  /**
   * Creates a QuestionSetting from JSON data during deserialization.
   *
   * @param settingKey the key identifying this setting within the question
   * @param settingTypeString identifier indicating how this setting will be used, as a string
   * @param localizedSettingDisplayName the display text for this setting, localized for different
   *     languages
   * @param settingValue the value for this setting
   * @param localizedSettingText additional text for this setting, localized for different languages
   * @return a new QuestionSetting instance
   */
  @JsonCreator
  public static QuestionSetting jsonCreator(
      @JsonProperty("settingKey") String settingKey,
      @JsonProperty("settingTypeString") String settingTypeString,
      @JsonProperty("localizedSettingDisplayName")
          Optional<LocalizedStrings> localizedSettingDisplayName,
      @JsonProperty("settingValue") Optional<String> settingValue,
      @JsonProperty("localizedSettingText") Optional<LocalizedStrings> localizedSettingText) {
    return QuestionSetting.create(
        settingKey,
        MapSettingType.valueOf(settingTypeString),
        localizedSettingDisplayName,
        settingValue,
        localizedSettingText);
  }

  /**
   * Create a {@link QuestionSetting} with a display name.
   *
   * @param settingKey the key identifying this setting within the question
   * @param settingType identifier indicating how this setting will be used, as a string
   * @param localizedSettingDisplayName the option's user-facing text (only required for FILTER
   *     settings)
   * @return the {@link QuestionSetting}
   */
  public static QuestionSetting create(
      String settingKey,
      SettingType settingType,
      Optional<LocalizedStrings> localizedSettingDisplayName) {
    return QuestionSetting.builder()
        .setSettingKey(settingKey)
        .setSettingTypeString(settingType.toString())
        .setLocalizedSettingDisplayName(localizedSettingDisplayName)
        .setSettingValue(Optional.empty())
        .setLocalizedSettingText(Optional.empty())
        .build();
  }

  /**
   * Create a {@link QuestionSetting} without a display name.
   *
   * @param settingKey the key identifying this setting within the question
   * @param settingType identifier indicating how this setting will be used
   * @return the {@link QuestionSetting}
   */
  public static QuestionSetting create(String settingKey, SettingType settingType) {
    return QuestionSetting.builder()
        .setSettingKey(settingKey)
        .setSettingTypeString(settingType.toString())
        .setLocalizedSettingDisplayName(Optional.empty())
        .setSettingValue(Optional.empty())
        .setLocalizedSettingText(Optional.empty())
        .build();
  }

  /**
   * Create a {@link QuestionSetting} with a display name, value, and additional text.
   *
   * @param settingKey the key identifying this setting within the question
   * @param settingType identifier indicating how this setting will be used
   * @param localizedSettingDisplayName the option's user-facing text
   * @param settingValue the value for this setting
   * @param localizedSettingText additional text for this setting
   * @return the {@link QuestionSetting}
   */
  public static QuestionSetting create(
      String settingKey,
      SettingType settingType,
      Optional<LocalizedStrings> localizedSettingDisplayName,
      Optional<String> settingValue,
      Optional<LocalizedStrings> localizedSettingText) {
    return QuestionSetting.builder()
        .setSettingKey(settingKey)
        .setSettingTypeString(settingType.toString())
        .setLocalizedSettingDisplayName(localizedSettingDisplayName)
        .setSettingValue(settingValue)
        .setLocalizedSettingText(localizedSettingText)
        .build();
  }

  public LocalizedQuestionSetting localize(Locale locale) {
    if (localizedSettingDisplayName().isEmpty()
        || !localizedSettingDisplayName().get().hasTranslationFor(locale)) {
      throw new RuntimeException(
          String.format("Locale %s not supported for question option %s", locale, this));
    }

    return localizeOrDefault(locale);
  }

  /**
   * Localize this question option for the given locale. If we cannot localize, use the default
   * locale.
   */
  public LocalizedQuestionSetting localizeOrDefault(Locale locale) {
    String displayName = "";
    String text = "";

    if (localizedSettingDisplayName().isPresent()) {
      try {
        displayName = localizedSettingDisplayName().get().get(locale);
      } catch (TranslationNotFoundException e) {
        displayName = localizedSettingDisplayName().get().getDefault();
        locale = LocalizedStrings.DEFAULT_LOCALE;
      }
    }

    if (localizedSettingText().isPresent()) {
      try {
        text = localizedSettingText().get().get(locale);
      } catch (TranslationNotFoundException e) {
        text = localizedSettingText().get().getDefault();
      }
    }

    return LocalizedQuestionSetting.create(
        settingKey(), settingType(), displayName, settingValue().orElse(""), text, locale);
  }

  public abstract Builder toBuilder();

  public static QuestionSetting.Builder builder() {
    return new AutoValue_QuestionSetting.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    @JsonProperty("localizedSettingDisplayName")
    public abstract Builder setLocalizedSettingDisplayName(
        Optional<LocalizedStrings> localizedSettingDisplayName);

    @JsonProperty("settingKey")
    public abstract Builder setSettingKey(String settingKey);

    @JsonProperty("settingTypeString")
    public abstract Builder setSettingTypeString(String settingTypeString);

    @JsonProperty("settingValue")
    public abstract Builder setSettingValue(Optional<String> settingValue);

    @JsonProperty("localizedSettingText")
    public abstract Builder setLocalizedSettingText(
        Optional<LocalizedStrings> localizedSettingText);

    public abstract QuestionSetting build();
  }
}
