package services.question.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;

/** Defines a currency question. */
public class CurrencyQuestionDefinition extends QuestionDefinition {

  public CurrencyQuestionDefinition(
      OptionalLong id,
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText) {
    super(
        id,
        name,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        CurrencyValidationPredicates.create());
  }

  public CurrencyQuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      CurrencyValidationPredicates validationPredicates) {
    super(name, enumeratorId, description, questionText, questionHelpText, validationPredicates);
  }

  public CurrencyQuestionDefinition(
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
        CurrencyValidationPredicates.create());
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
