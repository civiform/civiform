package services.question.types;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import services.Path;
import services.question.QuestionOption;

public class CheckboxQuestionDefinition extends MultiOptionQuestionDefinition {

  public CheckboxQuestionDefinition(
      OptionalLong id,
      String name,
      Path path,
      Optional<Long> enumeratorId,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableList<QuestionOption> options,
      MultiOptionValidationPredicates validationPredicates) {
    super(
        id,
        name,
        path,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        options,
        validationPredicates);
  }

  public CheckboxQuestionDefinition(
      String name,
      Path path,
      Optional<Long> enumeratorId,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableList<QuestionOption> options,
      MultiOptionValidationPredicates validationPredicates) {
    super(
        name,
        path,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        options,
        validationPredicates);
  }

  public CheckboxQuestionDefinition(
      String name,
      Path path,
      Optional<Long> enumeratorId,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableList<QuestionOption> options) {
    super(
        name,
        path,
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
