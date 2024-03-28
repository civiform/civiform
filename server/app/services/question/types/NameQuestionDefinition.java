package services.question.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;

/** Defines a name question. */
public final class NameQuestionDefinition extends QuestionDefinition {

  public NameQuestionDefinition(QuestionDefinitionConfig config) {
    super(config);
  }

  @AutoValue
  public abstract static class NameValidationPredicates extends ValidationPredicates {

    public static NameValidationPredicates parse(String jsonString) {
      try {
        // TODO: I think it's failing  because the existing JSON doesn't have the "type" property
        // in it so we can't parse it
        return mapper.readValue(
            jsonString, AutoValue_NameQuestionDefinition_NameValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static NameValidationPredicates create() {
      return new AutoValue_NameQuestionDefinition_NameValidationPredicates();
    }
  }

  public NameValidationPredicates getNameValidationPredicates() {
    return (NameValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.NAME;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return NameValidationPredicates.create();
  }
}
