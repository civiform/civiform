package services.question.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;

/** Defines a date question. */
public final class DateQuestionDefinition extends QuestionDefinition {

  public DateQuestionDefinition(@JsonProperty("config") QuestionDefinitionConfig config) {
    super(config);
  }

  @AutoValue
  public abstract static class DateValidationPredicates extends ValidationPredicates {

    public static DateQuestionDefinition.DateValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_DateQuestionDefinition_DateValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static DateQuestionDefinition.DateValidationPredicates create() {
      return new AutoValue_DateQuestionDefinition_DateValidationPredicates();
    }
  }

  public DateQuestionDefinition.DateValidationPredicates getDateValidationPredicates() {
    return (DateQuestionDefinition.DateValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.DATE;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return DateValidationPredicates.create();
  }
}
