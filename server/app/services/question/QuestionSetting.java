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

  @JsonProperty("settingKey")
  public abstract String settingKey();

  /** Identifier indicating how this setting will be used. */
  @JsonProperty("settingType")
  public abstract SettingType settingType();

  /**
   * The text strings to display to the user, keyed by locale. Only required for FILTER settings.
   */
  @JsonProperty("localizedSettingDisplayName")
  public abstract Optional<LocalizedStrings> localizedSettingDisplayName();

  /**
   * Creates a QuestionSetting from JSON data during deserialization.
   *
   * @param settingKey the unique key identifying this setting within the question
   * @param settingType identifier indicating how this setting will be used
   * @param localizedSettingDisplayName the display text for this setting, localized for different
   *     languages
   * @return a new QuestionSetting instance
   */
  @JsonCreator
  public static QuestionSetting jsonCreator(
      @JsonProperty("settingKey") String settingKey,
      @JsonProperty("settingType") SettingType settingType,
      @JsonProperty("localizedSettingDisplayName")
          Optional<LocalizedStrings> localizedSettingDisplayName) {
    return QuestionSetting.create(settingKey, settingType, localizedSettingDisplayName);
  }

  /**
   * Create a {@link QuestionSetting} with a display name.
   *
   * @param settingKey the unique key identifying this setting within the question
   * @param settingType identifier indicating how this setting will be used
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
        .setSettingType(settingType)
        .setLocalizedSettingDisplayName(localizedSettingDisplayName)
        .build();
  }

  /**
   * Create a {@link QuestionSetting} without a display name.
   *
   * @param settingKey the unique key identifying this setting within the question
   * @param settingType identifier indicating how this setting will be used
   * @return the {@link QuestionSetting}
   */
  public static QuestionSetting create(String settingKey, SettingType settingType) {
    return QuestionSetting.builder()
        .setSettingKey(settingKey)
        .setSettingType(settingType)
        .setLocalizedSettingDisplayName(Optional.empty())
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
    if (localizedSettingDisplayName().isEmpty()) {
      // For non-localized settings, use empty string as display text
      return LocalizedQuestionSetting.create(settingKey(), settingType(), "", locale);
    }

    try {
      String localizedText = localizedSettingDisplayName().get().get(locale);
      return LocalizedQuestionSetting.create(settingKey(), settingType(), localizedText, locale);
    } catch (TranslationNotFoundException e) {
      return LocalizedQuestionSetting.create(
          settingKey(),
          settingType(),
          localizedSettingDisplayName().get().getDefault(),
          LocalizedStrings.DEFAULT_LOCALE);
    }
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

    @JsonProperty("settingType")
    public abstract Builder setSettingType(SettingType settingType);

    public abstract QuestionSetting build();
  }
}
