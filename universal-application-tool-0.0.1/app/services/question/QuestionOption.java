package services.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import services.LocalizedStrings;

/**
 * Represents a single option in a {@link services.question.types.MultiOptionQuestionDefinition}.
 */
@AutoValue
public abstract class QuestionOption {

  /** Create a QuestionOption, used for JSON mapping. */
  @JsonCreator
  public static QuestionOption createForJsonMapping(
      @JsonProperty("id") long id,
      @JsonProperty("optionText") ImmutableMap<Locale, String> optionText,
      @JsonProperty("localizedOptionText") LocalizedStrings localizedOptionText) {
    if (!optionText.isEmpty()) {
      localizedOptionText = LocalizedStrings.create(optionText);
    }
    return QuestionOption.create(id, localizedOptionText);
  }

  /** Create a QuestionOption. */
  public static QuestionOption create(long id, LocalizedStrings optionText) {
    return new AutoValue_QuestionOption(id, optionText);
  }

  @JsonIgnore
  public LocalizedQuestionOption localize(Locale locale) {
    if (!optionText().hasTranslationFor(locale)) {
      throw new RuntimeException(
          String.format("Locale %s not supported for question option %s", locale, this));
    }

    return LocalizedQuestionOption.create(id(), optionText().getOrDefault(locale), locale);
  }

  /** The id for this option. */
  @JsonProperty("id")
  public abstract long id();

  /** The text strings to display to the user, keyed by locale. */
  @JsonProperty("optionText")
  public ImmutableMap<Locale, String> optionTextForJSON() {
    return optionText().translations();
  }

  @JsonProperty("localizedOptionText")
  public abstract LocalizedStrings optionText();
}
