package services.question.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;

public final class MapQuestionDefinition extends QuestionDefinition {

  public MapQuestionDefinition(@JsonProperty("config") QuestionDefinitionConfig config) {
    super(config);
  }

  @AutoValue
  public abstract static class MapValidationPredicates extends ValidationPredicates {

    public static MapValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_MapQuestionDefinition_MapValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static MapValidationPredicates create() {
      return new AutoValue_MapQuestionDefinition_MapValidationPredicates();
    }
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.MAP;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return MapValidationPredicates.create();
  }
}
