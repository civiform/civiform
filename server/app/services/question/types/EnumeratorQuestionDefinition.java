package services.question.types;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.OptionalInt;
import services.CiviFormError;
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

  @Override
  ImmutableSet<CiviFormError> internalValidate(Optional<QuestionDefinition> previousDefinition) {
    ImmutableSet.Builder<CiviFormError> errors = new ImmutableSet.Builder<>();
    if (getEntityType().hasEmptyTranslation()) {
      errors.add(CiviFormError.of("Enumerator question must have specified entity type"));
    }

    OptionalInt min = getMinEntities();
    OptionalInt max = getMaxEntities();
    if (min.isPresent() && min.getAsInt() < 0) {
      errors.add(CiviFormError.of("Minimum entity count cannot be negative"));
    }
    if (max.isPresent() && max.getAsInt() < 1) {
      errors.add(CiviFormError.of("Maximum entity count cannot be less than 1"));
    }
    if (min.isPresent() && max.isPresent() && min.getAsInt() > max.getAsInt()) {
      errors.add(
          CiviFormError.of(
              "Minimum entity count must be less than or equal to the maximum entity count"));
    }
    return errors.build();
  }

  public LocalizedStrings getEntityType() {
    return entityType;
  }

  @JsonIgnore
  public OptionalInt getMinEntities() {
    return getEnumeratorValidationPredicates().minEntities();
  }

  @JsonIgnore
  public OptionalInt getMaxEntities() {
    return getEnumeratorValidationPredicates().maxEntities();
  }

  private EnumeratorValidationPredicates getEnumeratorValidationPredicates() {
    return (EnumeratorValidationPredicates) getValidationPredicates();
  }

  @JsonDeserialize(
      builder = AutoValue_EnumeratorQuestionDefinition_EnumeratorValidationPredicates.Builder.class)
  @AutoValue
  public abstract static class EnumeratorValidationPredicates extends ValidationPredicates {

    public static EnumeratorValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString,
            AutoValue_EnumeratorQuestionDefinition_EnumeratorValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static EnumeratorValidationPredicates create() {
      return builder().build();
    }

    public static EnumeratorValidationPredicates create(int minEntities, int maxEntities) {
      return builder().setMinEntities(minEntities).setMaxEntities(maxEntities).build();
    }

    @JsonProperty("minEntities")
    public abstract OptionalInt minEntities();

    @JsonProperty("maxEntities")
    public abstract OptionalInt maxEntities();

    public static Builder builder() {
      return new AutoValue_EnumeratorQuestionDefinition_EnumeratorValidationPredicates.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      @JsonProperty("minEntities")
      public abstract Builder setMinEntities(OptionalInt minEntities);

      public abstract Builder setMinEntities(int minEntities);

      @JsonProperty("maxEntities")
      public abstract Builder setMaxEntities(OptionalInt maxEntities);

      public abstract Builder setMaxEntities(int maxEntities);

      public abstract EnumeratorValidationPredicates build();
    }
  }
}
