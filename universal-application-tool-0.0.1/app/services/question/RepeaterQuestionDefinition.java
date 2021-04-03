package services.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.OptionalLong;
import models.LifecycleStage;
import services.Path;

public class RepeaterQuestionDefinition extends QuestionDefinition {

  public RepeaterQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      RepeaterValidationPredicates validationPredicates) {
    super(
        id, version, name, path, description, lifecycleStage, questionText, questionHelpText, validationPredicates);
  }

  public RepeaterQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      RepeaterValidationPredicates validationPredicates) {
    super(version, name, path, description, lifecycleStage, questionText, questionHelpText, validationPredicates);
  }

  public RepeaterQuestionDefinition(
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
        RepeaterValidationPredicates.create());
  }

  @JsonDeserialize(
      builder = AutoValue_RepeaterQuestionDefinition_RepeaterValidationPredicates.Builder.class)
  @AutoValue
  public abstract static class RepeaterValidationPredicates extends ValidationPredicates {

    public static RepeaterValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_RepeaterQuestionDefinition_RepeaterValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static RepeaterValidationPredicates create() {
      return builder().build();
    }

    public static RepeaterValidationPredicates create(int minLength, int maxLength) {
      return builder().setMinLength(minLength).setMaxLength(maxLength).build();
    }

    @JsonProperty("minLength")
    public abstract OptionalInt minLength();

    @JsonProperty("maxLength")
    public abstract OptionalInt maxLength();

    public static Builder builder() {
      return new AutoValue_RepeaterQuestionDefinition_RepeaterValidationPredicates.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      @JsonProperty("minLength")
      public abstract Builder setMinLength(OptionalInt minLength);

      public abstract Builder setMinLength(int minLength);

      @JsonProperty("maxLength")
      public abstract Builder setMaxLength(OptionalInt maxLength);

      public abstract Builder setMaxLength(int maxLength);

      public abstract RepeaterValidationPredicates build();
    }
  }

  public RepeaterValidationPredicates getRepeaterValidationPredicates() {
    return (RepeaterValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.REPEATER;
  }

  @Override
  public ImmutableMap<Path, ScalarType> getScalarMap() {
    return ImmutableMap.of(getEntityPath(), getEntityType());
  }

  public Path getEntityPath() {
    return getPath().join("entity_name");
  }

  public ScalarType getEntityType() {
    return ScalarType.STRING;
  }

  public OptionalInt getMinLength() {
    return getRepeaterValidationPredicates().minLength();
  }

  public OptionalInt getMaxLength() {
    return getRepeaterValidationPredicates().maxLength();
  }
}
