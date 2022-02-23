package services.question.types;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;
import services.question.QuestionOption;

/** Defines a checkbox question. */
public class CheckboxQuestionDefinition extends MultiOptionQuestionDefinition {

  public CheckboxQuestionDefinition(
      OptionalLong id,
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      ImmutableList<QuestionOption> options,
      MultiOptionValidationPredicates validationPredicates) {
    super(
        id,
        name,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        options,
        validationPredicates);
  }

  public CheckboxQuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      ImmutableList<QuestionOption> options,
      MultiOptionValidationPredicates validationPredicates) {
    super(
        name,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        options,
        validationPredicates);
  }

  public CheckboxQuestionDefinition(
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
        MultiOptionValidationPredicates.create());
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.CHECKBOX;
  }
}
