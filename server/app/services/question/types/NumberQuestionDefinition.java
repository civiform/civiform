package services.question.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.OptionalLong;
import services.CiviFormError;

/** Defines a number question. */
public final class NumberQuestionDefinition extends QuestionDefinition {

  public NumberQuestionDefinition(@JsonProperty("config") QuestionDefinitionConfig config) {
    super(config);
  }

  @JsonDeserialize(
      builder = AutoValue_NumberQuestionDefinition_NumberValidationPredicates.Builder.class)
  @AutoValue
  public abstract static class NumberValidationPredicates extends ValidationPredicates {

    public static NumberValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_NumberQuestionDefinition_NumberValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static NumberValidationPredicates create() {
      return builder().build();
    }

    public static NumberValidationPredicates create(long min, long max) {
      return builder().setMin(min).setMax(max).build();
    }

    @JsonProperty("min")
    public abstract OptionalLong min();

    @JsonProperty("max")
    public abstract OptionalLong max();

    public static Builder builder() {
      return new AutoValue_NumberQuestionDefinition_NumberValidationPredicates.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      @JsonProperty("min")
      public abstract Builder setMin(OptionalLong min);

      public abstract Builder setMin(long min);

      @JsonProperty("max")
      public abstract Builder setMax(OptionalLong max);

      public abstract Builder setMax(long max);

      public abstract NumberValidationPredicates build();
    }
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.NUMBER;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return NumberValidationPredicates.create();
  }

  @Override
  ImmutableSet<CiviFormError> internalValidate(Optional<QuestionDefinition> previousDefinition) {
    ImmutableSet.Builder<CiviFormError> errors = new ImmutableSet.Builder<>();
    OptionalLong min = getMin();
    OptionalLong max = getMax();
    if (min.isPresent() && min.getAsLong() < 0) {
      errors.add(CiviFormError.of("Minimum value cannot be negative"));
    }
    if (max.isPresent() && max.getAsLong() < 0) {
      errors.add(CiviFormError.of("Maximum value cannot be negative"));
    }
    if (min.isPresent() && max.isPresent() && min.getAsLong() > max.getAsLong()) {
      errors.add(CiviFormError.of("Minimum value must be less than or equal to the maximum value"));
    }
    return errors.build();
  }

  @JsonIgnore
  public OptionalLong getMin() {
    return getNumberValidationPredicates().min();
  }

  @JsonIgnore
  public OptionalLong getMax() {
    return getNumberValidationPredicates().max();
  }

  @JsonIgnore
  private NumberValidationPredicates getNumberValidationPredicates() {
    return (NumberValidationPredicates) getValidationPredicates();
  }
}
