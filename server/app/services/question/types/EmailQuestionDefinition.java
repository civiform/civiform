package services.question.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;

/** Defines an email question. */
public final class EmailQuestionDefinition extends QuestionDefinition {

  public EmailQuestionDefinition(@JsonProperty("config") QuestionDefinitionConfig config) {
    super(config);
  }

  @AutoValue
  public abstract static class EmailValidationPredicates extends ValidationPredicates {

    public static EmailQuestionDefinition.EmailValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_EmailQuestionDefinition_EmailValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static EmailQuestionDefinition.EmailValidationPredicates create() {
      return new AutoValue_EmailQuestionDefinition_EmailValidationPredicates();
    }
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.EMAIL;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return EmailValidationPredicates.create();
  }
}
