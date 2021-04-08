package services.question.types;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import models.LifecycleStage;
import services.Path;

/**
 * Defines a dropdown question, which has a list of options, of which at most one and at least one
 * must be selected.
 */
public class DropdownQuestionDefinition extends MultiOptionQuestionDefinition {

  public DropdownQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      Optional<Long> repeaterId,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableListMultimap<Locale, String> options) {
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
        SINGLE_SELECT_PREDICATE);
  }

  public DropdownQuestionDefinition(
      long version,
      String name,
      Path path,
      Optional<Long> repeaterId,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableListMultimap<Locale, String> options) {
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
        SINGLE_SELECT_PREDICATE);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.DROPDOWN;
  }
}
