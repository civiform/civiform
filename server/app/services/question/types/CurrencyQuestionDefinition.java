package services.question.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;

/** Defines a currency question. */
public final class CurrencyQuestionDefinition extends QuestionDefinition {

  public CurrencyQuestionDefinition(@JsonProperty("config") QuestionDefinitionConfig config) {
    super(config);
  }

  @AutoValue
  public abstract static class CurrencyValidationPredicates extends ValidationPredicates {

    public static CurrencyValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_CurrencyQuestionDefinition_CurrencyValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static CurrencyValidationPredicates create() {
      return new AutoValue_CurrencyQuestionDefinition_CurrencyValidationPredicates();
    }
  }

  public CurrencyValidationPredicates getCurrencyValidationPredicates() {
    return (CurrencyValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.CURRENCY;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return CurrencyValidationPredicates.create();
  }
}
