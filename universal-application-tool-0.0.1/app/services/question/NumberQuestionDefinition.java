package services.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalLong;
import models.LifecycleStage;
import services.Path;

public class NumberQuestionDefinition extends QuestionDefinition {

  public NumberQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      NumberQuestionDefinition.NumberValidationPredicates validationPredicates) {
    super(
        id,
        version,
        name,
        path,
        description,
        lifecycleStage,
        questionText,
        questionHelpText,
        validationPredicates);
  }

  public NumberQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      NumberQuestionDefinition.NumberValidationPredicates validationPredicates) {
    super(
        version,
        name,
        path,
        description,
        lifecycleStage,
        questionText,
        questionHelpText,
        validationPredicates);
  }

  public NumberQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(
        version,
        name,
        path,
        description,
        lifecycleStage,
        questionText,
        questionHelpText,
        NumberQuestionDefinition.NumberValidationPredicates.create());
  }

  @JsonDeserialize(
      builder = AutoValue_NumberQuestionDefinition_NumberValidationPredicates.Builder.class)
  @AutoValue
  public abstract static class NumberValidationPredicates extends ValidationPredicates {

    public static NumberQuestionDefinition.NumberValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_NumberQuestionDefinition_NumberValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static NumberQuestionDefinition.NumberValidationPredicates create() {
      return builder().build();
    }

    public static NumberQuestionDefinition.NumberValidationPredicates create(int min, int max) {
      return builder().setMin(min).setMax(max).build();
    }

    @JsonProperty("min")
    public abstract OptionalLong min();

    @JsonProperty("max")
    public abstract OptionalLong max();

    public static NumberQuestionDefinition.NumberValidationPredicates.Builder builder() {
      return new AutoValue_NumberQuestionDefinition_NumberValidationPredicates.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      @JsonProperty("min")
      public abstract NumberQuestionDefinition.NumberValidationPredicates.Builder setMin(
          OptionalLong min);

      public abstract NumberQuestionDefinition.NumberValidationPredicates.Builder setMin(int min);

      @JsonProperty("max")
      public abstract NumberQuestionDefinition.NumberValidationPredicates.Builder setMax(
          OptionalLong max);

      public abstract NumberQuestionDefinition.NumberValidationPredicates.Builder setMax(int max);

      public abstract NumberQuestionDefinition.NumberValidationPredicates build();
    }
  }

  public NumberQuestionDefinition.NumberValidationPredicates getNumberValidationPredicates() {
    return (NumberQuestionDefinition.NumberValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.NUMBER;
  }

  @Override
  ImmutableMap<Path, ScalarType> getScalarMap() {
    return ImmutableMap.of(getNumberPath(), getNumberType());
  }

  public Path getNumberPath() {
    return getPath().join("number");
  }

  public ScalarType getNumberType() {
    return ScalarType.LONG;
  }

  public OptionalLong getMin() {
    return getNumberValidationPredicates().min();
  }

  public OptionalLong getMax() {
    return getNumberValidationPredicates().max();
  }
}
