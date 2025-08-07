package services.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.Locale;
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

  /** The text strings to display to the user, keyed by locale. */
  @JsonProperty("localizedSettingDisplayName")
  public abstract LocalizedStrings settingDisplayName();

  /**
   * Creates a QuestionSetting from JSON data during deserialization.
   *
   * @param settingKey the unique key identifying this setting within the question
   * @param localizedSettingDisplayName the display text for this setting, localized for different
   *     languages
   * @return a new QuestionSetting instance
   */
  @JsonCreator
  public static QuestionSetting jsonCreator(
      @JsonProperty("settingKey") String settingKey,
      @JsonProperty("localizedSettingDisplayName") LocalizedStrings localizedSettingDisplayName) {
    return QuestionSetting.create(settingKey, localizedSettingDisplayName);
  }

  /**
   * Create a {@link QuestionSetting}.
   *
   * @param settingDisplayName the option's user-facing text
   * @return the {@link QuestionSetting}
   */
  public static QuestionSetting create(String settingKey, LocalizedStrings settingDisplayName) {
    return QuestionSetting.builder()
        .setSettingKey(settingKey)
        .setSettingDisplayName(settingDisplayName)
        .build();
  }

  public LocalizedQuestionSetting localize(Locale locale) {
    if (!settingDisplayName().hasTranslationFor(locale)) {
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
    try {
      String localizedText = settingDisplayName().get(locale);
      return LocalizedQuestionSetting.create(settingKey(), localizedText, locale);
    } catch (TranslationNotFoundException e) {
      return LocalizedQuestionSetting.create(
          settingKey(), settingDisplayName().getDefault(), LocalizedStrings.DEFAULT_LOCALE);
    }
  }

  public abstract Builder toBuilder();

  public static QuestionSetting.Builder builder() {
    return new AutoValue_QuestionSetting.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    @JsonProperty("localizedSettingDisplayName")
    public abstract Builder setSettingDisplayName(LocalizedStrings settingDisplayName);

    @JsonProperty("settingKey")
    public abstract Builder setSettingKey(String settingKey);

    public abstract QuestionSetting build();
  }
}
