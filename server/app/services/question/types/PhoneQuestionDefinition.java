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
        QuestionDefinitionConfig.builder()
            .setId(id)
            .setName(name)
            .setEnumeratorId(enumeratorId)
            .setDescription(description)
            .setQuestionText(questionText)
            .setQuestionHelpText(questionHelpText)
            .setValidationPredicates(validationPredicates)
            .setLastModifiedTime(lastModifiedTime)
            .build());
  }

  public PhoneQuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      PhoneQuestionDefinition.PhoneValidationPredicates validationPredicates) {
    super(
        QuestionDefinitionConfig.builder()
            .setName(name)
            .setEnumeratorId(enumeratorId)
            .setDescription(description)
            .setQuestionText(questionText)
            .setQuestionHelpText(questionHelpText)
            .setValidationPredicates(validationPredicates)
            .build());
  }

  public PhoneQuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText) {
    super(
        QuestionDefinitionConfig.builder()
            .setName(name)
            .setEnumeratorId(enumeratorId)
            .setDescription(description)
            .setQuestionText(questionText)
            .setQuestionHelpText(questionHelpText)
            .setValidationPredicates(PhoneQuestionDefinition.PhoneValidationPredicates.create())
            .build());
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
}
