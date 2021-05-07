package services.question;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import services.LocalizationUtils;

/**
 * Represents a single option in a {@link services.question.types.MultiOptionQuestionDefinition}.
 */
@JsonDeserialize(builder = AutoValue_QuestionOption.Builder.class)
@AutoValue
public abstract class QuestionOption {

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

  public static QuestionOption.Builder builder() {
    return new AutoValue_QuestionOption.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    @JsonProperty("id")
    public abstract Builder setId(long id);

    @JsonProperty("optionText")
    public abstract Builder setOptionText(ImmutableMap<Locale, String> optionText);

    public abstract ImmutableMap.Builder<Locale, String> optionTextBuilder();

    public abstract QuestionOption build();

    /**
     * Update an existing localization of option text. This will overwrite the old name for that
     * locale.
     */
    public Builder updateOptionText(
        ImmutableMap<Locale, String> existing, Locale locale, String text) {
      if (existing.containsKey(locale)) {
        setOptionText(LocalizationUtils.overwriteExistingTranslation(existing, locale, text));
      } else {
        optionTextBuilder().put(locale, text);
      }
      return this;
    }
  }
}
