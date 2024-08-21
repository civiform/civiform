package services.question.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.OptionalInt;
import services.CiviFormError;

/** Defines an id question. */
public final class IdQuestionDefinition extends QuestionDefinition {

  public IdQuestionDefinition(@JsonProperty("config") QuestionDefinitionConfig config) {
    super(config);
  }

  @JsonDeserialize(builder = AutoValue_IdQuestionDefinition_IdValidationPredicates.Builder.class)
  @AutoValue
  public abstract static class IdValidationPredicates extends ValidationPredicates {

    public static IdValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_IdQuestionDefinition_IdValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static IdValidationPredicates create() {
      return builder().build();
    }

    public static IdValidationPredicates create(int minLength, int maxLength) {
      return builder().setMinLength(minLength).setMaxLength(maxLength).build();
    }

    @JsonProperty("minLength")
    public abstract OptionalInt minLength();

    @JsonProperty("maxLength")
    public abstract OptionalInt maxLength();

    public static Builder builder() {
      return new AutoValue_IdQuestionDefinition_IdValidationPredicates.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      @JsonProperty("minLength")
      public abstract Builder setMinLength(OptionalInt minLength);

      public abstract Builder setMinLength(int minLength);

      @JsonProperty("maxLength")
      public abstract Builder setMaxLength(OptionalInt maxLength);

      public abstract Builder setMaxLength(int maxLength);

      public abstract IdValidationPredicates build();
    }
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ID;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return IdValidationPredicates.create();
  }

  @Override
  ImmutableSet<CiviFormError> internalValidate(Optional<QuestionDefinition> previousDefinition) {
    ImmutableSet.Builder<CiviFormError> errors = new ImmutableSet.Builder<>();
    OptionalInt min = getMinLength();
    OptionalInt max = getMaxLength();
    if (min.isPresent() && min.getAsInt() < 0) {
      errors.add(CiviFormError.of("Minimum length cannot be negative"));
    }
    if (max.isPresent() && max.getAsInt() < 1) {
      errors.add(CiviFormError.of("Maximum length cannot be less than 1"));
    }
    if (min.isPresent() && max.isPresent() && min.getAsInt() > max.getAsInt()) {
      errors.add(
          CiviFormError.of("Minimum length must be less than or equal to the maximum length"));
    }
    return errors.build();
  }

  @JsonIgnore
  public OptionalInt getMinLength() {
    return getIdValidationPredicates().minLength();
  }

  @JsonIgnore
  public OptionalInt getMaxLength() {
    return getIdValidationPredicates().maxLength();
  }

  @JsonIgnore
  private IdValidationPredicates getIdValidationPredicates() {
    return (IdValidationPredicates) getValidationPredicates();
  }
}
