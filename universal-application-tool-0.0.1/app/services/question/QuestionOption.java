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

  /** The id for this option. */
  @JsonProperty("id")
  public abstract long id();

  /** The text strings to display to the user, keyed by locale. */
  @JsonProperty("localizedOptionText")
  public abstract LocalizedStrings optionText();

  /**
   * Create a QuestionOption, used for JSON mapping to account for the legacy `optionText`.
   *
   * <p>Legacy QuestionOptions from before early May 2021 will not have `localizedOptionText`.
   */
  @JsonCreator
  public static QuestionOption jsonCreator(
      @JsonProperty("id") long id,
      @JsonProperty("optionText") ImmutableMap<Locale, String> optionText,
      @JsonProperty("localizedOptionText") LocalizedStrings localizedOptionText) {
    if (localizedOptionText != null) {
      return QuestionOption.create(id, localizedOptionText);
    }
    return QuestionOption.create(id, LocalizedStrings.create(optionText));
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
}
