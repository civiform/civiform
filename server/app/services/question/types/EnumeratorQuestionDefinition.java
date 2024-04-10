package services.question.types;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
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

  static final String DEFAULT_ENTITY_TYPE = "Item";

  @JsonProperty("entityType")
  private final LocalizedStrings entityType;

  public EnumeratorQuestionDefinition(
      @JsonProperty("config") QuestionDefinitionConfig config,
      @JsonProperty("entityType") LocalizedStrings entityType) {
    super(config);
    this.entityType = checkNotNull(entityType);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ENUMERATOR;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return EnumeratorValidationPredicates.create();
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
