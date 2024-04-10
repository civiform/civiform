package services.question.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;

public final class PhoneQuestionDefinition extends QuestionDefinition {

  public PhoneQuestionDefinition(@JsonProperty("config") QuestionDefinitionConfig config) {
    super(config);
  }

  @AutoValue
  public abstract static class PhoneValidationPredicates
      extends QuestionDefinition.ValidationPredicates {

    public static PhoneQuestionDefinition.PhoneValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_PhoneQuestionDefinition_PhoneValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static PhoneQuestionDefinition.PhoneValidationPredicates create() {
      return new AutoValue_PhoneQuestionDefinition_PhoneValidationPredicates();
    }
  }

  public PhoneQuestionDefinition.PhoneValidationPredicates getPhoneValidationPredicates() {
    return (PhoneQuestionDefinition.PhoneValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.PHONE;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return PhoneValidationPredicates.create();
  }
}
