package services.question.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.OptionalLong;

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

  public NumberValidationPredicates getNumberValidationPredicates() {
    return (NumberValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.NUMBER;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return NumberValidationPredicates.create();
  }

  public OptionalLong getMin() {
    return getNumberValidationPredicates().min();
  }

  public OptionalLong getMax() {
    return getNumberValidationPredicates().max();
  }
}
