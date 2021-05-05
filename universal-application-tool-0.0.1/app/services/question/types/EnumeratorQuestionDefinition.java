package services.question.types;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import services.Path;

/**
 * Enumerator questions provide a variable list of user-defined identifiers for some repeated
 * entity. Examples of repeated entities could be household members, vehicles, jobs, etc.
 *
 * <p>An enumerator question definition can be referenced by other question definitions that
 * themselves repeat for each of the enumerator-defined entities. For example, an enumerator for
 * vehicles may ask the user to identify each of their vehicles, with other questions referencing it
 * that ask about each vehicle's make, model, and year.
 */
public class EnumeratorQuestionDefinition extends QuestionDefinition {

  public EnumeratorQuestionDefinition(
      OptionalLong id,
      String name,
      Path path,
      Optional<Long> enumeratorId,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(
        id,
        name,
        path,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        EnumeratorValidationPredicates.create());
  }

  public EnumeratorQuestionDefinition(
      String name,
      Path path,
      Optional<Long> enumeratorId,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(
        name,
        path,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        EnumeratorValidationPredicates.create());
  }

  public EnumeratorValidationPredicates getEnumeratorValidationPredicates() {
    return (EnumeratorValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ENUMERATOR;
  }

  @Override
  public ImmutableMap<Path, ScalarType> getScalarMap() {
    return ImmutableMap.of();
  }

  @AutoValue
  public abstract static class EnumeratorValidationPredicates extends ValidationPredicates {
    public static EnumeratorValidationPredicates create() {
      return new AutoValue_EnumeratorQuestionDefinition_EnumeratorValidationPredicates();
    }
  }
}
