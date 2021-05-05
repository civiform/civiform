package services.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import services.LocalizationUtils;

/**
 * Represents a single option in a {@link services.question.types.MultiOptionQuestionDefinition}.
 */
@AutoValue
public abstract class QuestionOption {

  /** Create a QuestionOption. */
  @JsonCreator
  public static QuestionOption create(
      @JsonProperty("id") long id,
      @JsonProperty("optionText") ImmutableMap<Locale, String> optionText) {
    return new AutoValue_QuestionOption(id, optionText);
  }

  @JsonIgnore
  public LocalizedQuestionOption localize(Locale locale) {
    if (!optionText().containsKey(locale)) {
      throw new RuntimeException(
          String.format("Locale %s not supported for question option %s", locale, this));
    }

    return LocalizedQuestionOption.create(id(), optionText().get(locale), locale);
  }

  /** The id for this option. */
  @JsonProperty("id")
  public abstract long id();

  /** The text strings to display to the user, keyed by locale. */
  @JsonProperty("optionText")
  public abstract ImmutableMap<Locale, String> optionText();

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(long id);

    public abstract Builder setOptionText(ImmutableMap<Locale, String> optionText);

    public abstract ImmutableMap.Builder<Locale, String> optionTextBuilder();

    public abstract QuestionOption build();

    /**
     * Add a new option text localization. This will fail if a translation for the given locale
     * already exists.
     */
    public Builder addLocalizedOptionText(Locale locale, String text) {
      optionTextBuilder().put(locale, text);
      return this;
    }

    /**
     * Update an existing localization of option text. This will overwrite the old name for that
     * locale.
     */
    public Builder updateOptionText(
        ImmutableMap<Locale, String> existing, Locale locale, String text) {
      if (existing.containsKey(locale)) {
        setOptionText(LocalizationUtils.overwriteExistingTranslation(existing, locale, text));
      } else {
        addLocalizedOptionText(locale, text);
      }
      return this;
    }
  }
}
