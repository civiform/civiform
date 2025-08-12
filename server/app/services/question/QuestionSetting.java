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
  @JsonProperty("settingId")
  public abstract SettingType settingId();

  /**
   * The text strings to display to the user, keyed by locale. Only required for FILTER settings.
   */
  @JsonProperty("localizedSettingDisplayName")
  public abstract Optional<LocalizedStrings> localizedSettingDisplayName();

  /**
   * Creates a QuestionSetting from JSON data during deserialization.
   *
   * @param settingKey the unique key identifying this setting within the question
   * @param settingId identifier indicating how this setting will be used
   * @param localizedSettingDisplayName the display text for this setting, localized for different
   *     languages
   * @return a new QuestionSetting instance
   */
  @JsonCreator
  public static QuestionSetting jsonCreator(
      @JsonProperty("settingKey") String settingKey,
      @JsonProperty("settingId") SettingType settingId,
      @JsonProperty("localizedSettingDisplayName")
          Optional<LocalizedStrings> localizedSettingDisplayName) {
    return QuestionSetting.create(settingKey, settingId, localizedSettingDisplayName);
  }

  /**
   * Create a {@link QuestionSetting}.
   *
   * @param settingKey the unique key identifying this setting within the question
   * @param settingId identifier indicating how this setting will be used
   * @param localizedSettingDisplayName the option's user-facing text (only required for FILTER
   *     settings)
   * @return the {@link QuestionSetting}
   */
  public static QuestionSetting create(
      String settingKey,
      SettingType settingId,
      Optional<LocalizedStrings> localizedSettingDisplayName) {
    return QuestionSetting.builder()
        .setSettingKey(settingKey)
        .setSettingId(settingId)
        .setLocalizedSettingDisplayName(localizedSettingDisplayName)
        .build();
  }

  /**
   * Create a {@link QuestionSetting} for FILTER type with localized display name.
   *
   * @param settingKey the unique key identifying this setting within the question
   * @param localizedSettingDisplayName the user-facing text for the filter
   * @return the {@link QuestionSetting}
   */
  public static QuestionSetting createFilter(
      String settingKey, LocalizedStrings localizedSettingDisplayName) {
    return create(settingKey, SettingType.FILTER, Optional.of(localizedSettingDisplayName));
  }

  /**
   * Create a {@link QuestionSetting} for mapping types (NAME, ADDRESS, URL) without display name.
   *
   * @param settingKey the GeoJSON field name this setting maps to
   * @param settingType the type of mapping (NAME, ADDRESS, or URL)
   * @return the {@link QuestionSetting}
   */
  public static QuestionSetting createMapping(String settingKey, SettingType settingType) {
    if (settingType == SettingType.FILTER) {
      throw new IllegalArgumentException("Use createFilter() for FILTER settings");
    }
    return create(settingKey, settingType, Optional.empty());
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
      return LocalizedQuestionSetting.create(settingKey(), settingId(), "", locale);
    }

    try {
      String localizedText = localizedSettingDisplayName().get().get(locale);
      return LocalizedQuestionSetting.create(settingKey(), settingId(), localizedText, locale);
    } catch (TranslationNotFoundException e) {
      return LocalizedQuestionSetting.create(
          settingKey(),
          settingId(),
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

    @JsonProperty("settingId")
    public abstract Builder setSettingId(SettingType settingId);

    public abstract QuestionSetting build();
  }
}
