package services.question.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;

/** Defines a currency question. */
public final class CurrencyQuestionDefinition extends QuestionDefinition {

  public CurrencyQuestionDefinition(
      OptionalLong id,
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      Optional<Instant> lastModifiedTime) {
    super(
        QuestionDefinitionConfig.builder()
            .setId(id)
            .setName(name)
            .setEnumeratorId(enumeratorId)
            .setDescription(description)
            .setQuestionText(questionText)
            .setQuestionHelpText(questionHelpText)
            .setLastModifiedTime(lastModifiedTime)
            .build());
  }

  public CurrencyQuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      CurrencyValidationPredicates validationPredicates) {
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

  public CurrencyQuestionDefinition(
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
            .build());
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
}
