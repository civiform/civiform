package services.question.types;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;

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

  protected static final String DEFAULT_ENTITY_TYPE = "Item";

  private final LocalizedStrings entityType;

  public EnumeratorQuestionDefinition(
      OptionalLong id,
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      LocalizedStrings entityType) {
    super(
        id,
        name,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        EnumeratorValidationPredicates.create());
    this.entityType = checkNotNull(entityType);
  }

  public EnumeratorQuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      LocalizedStrings entityType) {
    super(
        name,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        EnumeratorValidationPredicates.create());
    this.entityType = checkNotNull(entityType);
  }

  public EnumeratorValidationPredicates getEnumeratorValidationPredicates() {
    return (EnumeratorValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ENUMERATOR;
  }

  public LocalizedStrings getEntityType() {
    return entityType;
  }

  @AutoValue
  public abstract static class EnumeratorValidationPredicates extends ValidationPredicates {
    public static EnumeratorValidationPredicates create() {
      return new AutoValue_EnumeratorQuestionDefinition_EnumeratorValidationPredicates();
    }
  }
}
