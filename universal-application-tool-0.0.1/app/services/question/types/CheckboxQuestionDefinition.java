package services.question.types;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import models.LifecycleStage;
import services.Path;
import services.question.QuestionOption;

public class CheckboxQuestionDefinition extends MultiOptionQuestionDefinition {

  public CheckboxQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      Optional<Long> repeaterId,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableList<QuestionOption> options,
      MultiOptionValidationPredicates validationPredicates) {
    super(
        id,
        version,
        name,
        path,
        repeaterId,
        description,
        lifecycleStage,
        questionText,
        questionHelpText,
        options,
        validationPredicates);
  }

  public CheckboxQuestionDefinition(
      long version,
      String name,
      Path path,
      Optional<Long> repeaterId,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableList<QuestionOption> options,
      MultiOptionValidationPredicates validationPredicates) {
    super(
        version,
        name,
        path,
        repeaterId,
        description,
        lifecycleStage,
        questionText,
        questionHelpText,
        options,
        validationPredicates);
  }

  public CheckboxQuestionDefinition(
      long version,
      String name,
      Path path,
      Optional<Long> repeaterId,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableList<QuestionOption> options) {
    super(
        version,
        name,
        path,
        repeaterId,
        description,
        lifecycleStage,
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
