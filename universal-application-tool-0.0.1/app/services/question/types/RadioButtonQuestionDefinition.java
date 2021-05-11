package services.question.types;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;
import services.Path;
import services.question.QuestionOption;

public class RadioButtonQuestionDefinition extends MultiOptionQuestionDefinition {

  public RadioButtonQuestionDefinition(
      OptionalLong id,
      String name,
      Path path,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      ImmutableList<QuestionOption> options) {
    super(
        id,
        name,
        path,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        options,
        SINGLE_SELECT_PREDICATE);
  }

  public RadioButtonQuestionDefinition(
      String name,
      Path path,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      ImmutableList<QuestionOption> options) {
    super(
        name,
        path,
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
