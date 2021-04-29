package services.question.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import services.Path;

public class NameQuestionDefinition extends QuestionDefinition {

  public NameQuestionDefinition(
      OptionalLong id,
      String name,
      Path path,
      Optional<Long> repeaterId,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      NameValidationPredicates validationPredicates) {
    super(
        id,
        name,
        path,
        repeaterId,
        description,
        questionText,
        questionHelpText,
        validationPredicates);
  }

  public NameQuestionDefinition(
      String name,
      Path path,
      Optional<Long> repeaterId,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      NameValidationPredicates validationPredicates) {
    super(
        name, path, repeaterId, description, questionText, questionHelpText, validationPredicates);
  }

  public NameQuestionDefinition(
      String name,
      Path path,
      Optional<Long> repeaterId,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(
        name,
        path,
        repeaterId,
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

  @Override
  ImmutableMap<Path, ScalarType> getScalarMap() {
    return ImmutableMap.of(
        getFirstNamePath(),
        getFirstNameType(),
        getMiddleNamePath(),
        getMiddleNameType(),
        getLastNamePath(),
        getLastNameType());
  }

  public Path getFirstNamePath() {
    return getPath().join("first_name");
  }

  public ScalarType getFirstNameType() {
    return ScalarType.STRING;
  }

  public Path getMiddleNamePath() {
    return getPath().join("middle_name");
  }

  public ScalarType getMiddleNameType() {
    return ScalarType.STRING;
  }

  public Path getLastNamePath() {
    return getPath().join("last_name");
  }

  public ScalarType getLastNameType() {
    return ScalarType.STRING;
  }
}
