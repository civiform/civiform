package services.question;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;

/**
 * Represents a single option in a {@link services.question.types.MultiOptionQuestionDefinition}.
 */
@AutoValue
public abstract class QuestionOption {

  /** Create a QuestionOption. */
  public static QuestionOption create(long id, ImmutableMap<Locale, String> optionText) {
    return new AutoValue_QuestionOption(id, optionText);
  }

  public LocalizedQuestionOption localize(Locale locale) {
    return LocalizedQuestionOption.create(id(), optionText().get(locale), locale);
  }

  /** The id for this option. */
  public abstract long id();

  /** The text strings to display to the user, keyed by locale. */
  public abstract ImmutableMap<Locale, String> optionText();
}
