package services.question.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;

public final class PhoneQuestionDefinition extends QuestionDefinition {
  public PhoneQuestionDefinition(
      OptionalLong id,
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      PhoneQuestionDefinition.PhoneValidationPredicates validationPredicates,
      Optional<Instant> lastModifiedTime) {
    super(
        id,
        name,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        validationPredicates,
        lastModifiedTime);
  }

  public PhoneQuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      PhoneQuestionDefinition.PhoneValidationPredicates validationPredicates) {
    super(name, enumeratorId, description, questionText, questionHelpText, validationPredicates);
  }

  public PhoneQuestionDefinition(
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
        PhoneQuestionDefinition.PhoneValidationPredicates.create());
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
    return QuestionType.NAME;
  }
}
