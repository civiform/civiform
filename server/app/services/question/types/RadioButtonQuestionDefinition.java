package services.question.types;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;
import services.question.QuestionOption;

/** Defines a radio button question. */
public final class RadioButtonQuestionDefinition extends MultiOptionQuestionDefinition {

  public RadioButtonQuestionDefinition(
      OptionalLong id,
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      ImmutableList<QuestionOption> options,
      Optional<Instant> lastModifiedTime) {
    super(
        id,
        name,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        options,
        SINGLE_SELECT_PREDICATE,
        lastModifiedTime);
  }

  public RadioButtonQuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      ImmutableList<QuestionOption> options) {
    super(
        name,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        options,
        SINGLE_SELECT_PREDICATE);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.RADIO_BUTTON;
  }
}
