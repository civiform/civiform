package services.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;

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
    return LocalizedQuestionOption.create(id(), optionText().get(locale), locale);
  }

  /** The id for this option. */
  @JsonProperty("id")
  public abstract long id();

  /** The text strings to display to the user, keyed by locale. */
  @JsonProperty("optionText")
  public abstract ImmutableMap<Locale, String> optionText();
}
