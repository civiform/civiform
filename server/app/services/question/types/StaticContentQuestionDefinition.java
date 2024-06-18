package services.question.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;

/**
 * Defines a static content question. A static content question displays static content without
 * asking for an answer.
 */
public final class StaticContentQuestionDefinition extends QuestionDefinition {

  public StaticContentQuestionDefinition(@JsonProperty("config") QuestionDefinitionConfig config) {
    super(config);
  }

  @AutoValue
  public abstract static class StaticContentValidationPredicates extends ValidationPredicates {

    public static StaticContentValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString,
            AutoValue_StaticContentQuestionDefinition_StaticContentValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static StaticContentValidationPredicates create() {
      return new AutoValue_StaticContentQuestionDefinition_StaticContentValidationPredicates();
    }
  }

  public StaticContentValidationPredicates getStaticContentValidationPredicates() {
    return (StaticContentValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.STATIC;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return StaticContentValidationPredicates.create();
  }
}
