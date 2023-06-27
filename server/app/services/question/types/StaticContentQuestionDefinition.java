package services.question.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;

/**
 * Defines a static content question. A static content question displays static content without
 * asking for an answer.
 */
public final class StaticContentQuestionDefinition extends QuestionDefinition {

  public StaticContentQuestionDefinition(QuestionDefinitionConfig config) {
    super(config);
  }

  @AutoValue
  public abstract static class StaticContentValidationPredicates extends ValidationPredicates {

    public static StaticContentQuestionDefinition.StaticContentValidationPredicates parse(
        String jsonString) {
      try {
        return mapper.readValue(
            jsonString,
            AutoValue_StaticContentQuestionDefinition_StaticContentValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static StaticContentQuestionDefinition.StaticContentValidationPredicates create() {
      return new AutoValue_StaticContentQuestionDefinition_StaticContentValidationPredicates();
    }
  }

  public StaticContentQuestionDefinition.StaticContentValidationPredicates
      getStaticContentValidationPredicates() {
    return (StaticContentQuestionDefinition.StaticContentValidationPredicates)
        getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.STATIC;
  }
}
