package services.question.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;

/** Defines a date question. */
public class DateQuestionDefinition extends QuestionDefinition {

  public DateQuestionDefinition(
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
        DateValidationPredicates.create());
  }

  public DateQuestionDefinition(
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
        DateValidationPredicates.create());
  }

  @AutoValue
  public abstract static class DateValidationPredicates extends ValidationPredicates {

    public static DateQuestionDefinition.DateValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_DateQuestionDefinition_DateValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static DateQuestionDefinition.DateValidationPredicates create() {
      return new AutoValue_DateQuestionDefinition_DateValidationPredicates();
    }
  }

  public DateQuestionDefinition.DateValidationPredicates getDateValidationPredicates() {
    return (DateQuestionDefinition.DateValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.DATE;
  }
}
