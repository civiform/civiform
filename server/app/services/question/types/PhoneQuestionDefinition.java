package services.question.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;

public final class PhoneQuestionDefinition extends QuestionDefinition {

  public PhoneQuestionDefinition(QuestionDefinitionConfig config) {
    super(config);
  }

  @AutoValue
  public abstract static class PhoneValidationPredicates extends ValidationPredicates {

    public static PhoneValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_PhoneQuestionDefinition_PhoneValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static PhoneValidationPredicates create() {
      return new AutoValue_PhoneQuestionDefinition_PhoneValidationPredicates();
    }
  }

  public PhoneValidationPredicates getPhoneValidationPredicates() {
    return (PhoneValidationPredicates) getValidationPredicates();
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
