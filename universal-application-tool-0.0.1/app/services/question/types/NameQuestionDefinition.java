package services.question.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;

/** Defines a name question. */
public class NameQuestionDefinition extends QuestionDefinition {

  public NameQuestionDefinition(
      OptionalLong id,
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      NameValidationPredicates validationPredicates) {
    super(
        id, name, enumeratorId, description, questionText, questionHelpText, validationPredicates);
  }

  public NameQuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      NameValidationPredicates validationPredicates) {
    super(name, enumeratorId, description, questionText, questionHelpText, validationPredicates);
  }

  public NameQuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText) {
    super(
        name,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        NameValidationPredicates.create());
  }

  @AutoValue
  public abstract static class NameValidationPredicates extends ValidationPredicates {

    public static NameValidationPredicates parse(String jsonString) {
      try {
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
}
