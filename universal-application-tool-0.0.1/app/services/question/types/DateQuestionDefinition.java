package services.question.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import services.Path;

public class DateQuestionDefinition extends QuestionDefinition {

  public DateQuestionDefinition(
      String name,
      Path path,
      Optional<Long> enumeratorId,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(
        name,
        path,
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

  @Override
  ImmutableMap<Path, ScalarType> getScalarMap() {
    return ImmutableMap.of();
  }
}
