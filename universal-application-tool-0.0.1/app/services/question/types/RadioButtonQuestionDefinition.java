package services.question.types;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import services.Path;
import services.question.QuestionOption;

public class RadioButtonQuestionDefinition extends MultiOptionQuestionDefinition {

  public RadioButtonQuestionDefinition(
      OptionalLong id,
      String name,
      Path path,
      Optional<Long> repeaterId,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableList<QuestionOption> options) {
    super(
        id,
        name,
        path,
        repeaterId,
        description,
        questionText,
        questionHelpText,
        options,
        SINGLE_SELECT_PREDICATE);
  }

  public RadioButtonQuestionDefinition(
      String name,
      Path path,
      Optional<Long> repeaterId,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableList<QuestionOption> options) {
    super(
        name,
        path,
        repeaterId,
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
