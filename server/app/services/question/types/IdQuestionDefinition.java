package services.question.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.OptionalInt;

/** Defines an id question. */
public final class IdQuestionDefinition extends QuestionDefinition {

  public IdQuestionDefinition(QuestionDefinitionConfig config) {
    super(config);
  }

  @JsonDeserialize(builder = AutoValue_IdQuestionDefinition_IdValidationPredicates.Builder.class)
  @AutoValue
  @JsonTypeName("idthing")
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

  public IdValidationPredicates getIdValidationPredicates() {
    return (IdValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ID;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return IdValidationPredicates.create();
  }

  public OptionalInt getMinLength() {
    return getIdValidationPredicates().minLength();
  }

  public OptionalInt getMaxLength() {
    return getIdValidationPredicates().maxLength();
  }
}
