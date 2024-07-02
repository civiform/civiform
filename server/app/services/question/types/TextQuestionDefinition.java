package services.question.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.OptionalInt;
import services.CiviFormError;

/** Defines a text question. */
public final class TextQuestionDefinition extends QuestionDefinition {

  public TextQuestionDefinition(@JsonProperty("config") QuestionDefinitionConfig config) {
    super(config);
  }

  @JsonDeserialize(
      builder = AutoValue_TextQuestionDefinition_TextValidationPredicates.Builder.class)
  @AutoValue
  public abstract static class TextValidationPredicates extends ValidationPredicates {

    public static TextValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_TextQuestionDefinition_TextValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static TextValidationPredicates create() {
      return builder().build();
    }

    public static TextValidationPredicates create(int minLength, int maxLength) {
      return builder().setMinLength(minLength).setMaxLength(maxLength).build();
    }

    @JsonProperty("minLength")
    public abstract OptionalInt minLength();

    @JsonProperty("maxLength")
    public abstract OptionalInt maxLength();

    public static Builder builder() {
      return new AutoValue_TextQuestionDefinition_TextValidationPredicates.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      @JsonProperty("minLength")
      public abstract Builder setMinLength(OptionalInt minLength);

      public abstract Builder setMinLength(int minLength);

      @JsonProperty("maxLength")
      public abstract Builder setMaxLength(OptionalInt maxLength);

      public abstract Builder setMaxLength(int maxLength);

      public abstract TextValidationPredicates build();
    }
  }

  public TextValidationPredicates getTextValidationPredicates() {
    return (TextValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.TEXT;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return TextValidationPredicates.create();
  }

  @Override
  ImmutableSet<CiviFormError> internalValidate(Optional<QuestionDefinition> previousDefinition) {
    ImmutableSet.Builder<CiviFormError> errors = new ImmutableSet.Builder<>();
    OptionalInt min = getMinLength();
    OptionalInt max = getMaxLength();
    if (min.isPresent() && max.isPresent() && min.getAsInt() > max.getAsInt()) {
      errors.add(
          CiviFormError.of("Minimum length must be less than or equal to the maximum length"));
    }
    return errors.build();
  }

  public OptionalInt getMinLength() {
    return getTextValidationPredicates().minLength();
  }

  public OptionalInt getMaxLength() {
    return getTextValidationPredicates().maxLength();
  }
}
