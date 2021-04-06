package services.question;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalLong;
import models.LifecycleStage;
import services.Path;

public class NameQuestionDefinition extends QuestionDefinition {

  public NameQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      NameValidationPredicates validationPredicates) {
    super(
        id,
        version,
        name,
        path,
        description,
        lifecycleStage,
        questionText,
        questionHelpText,
        validationPredicates);
  }

  public NameQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      NameValidationPredicates validationPredicates) {
    super(
        version,
        name,
        path,
        description,
        lifecycleStage,
        questionText,
        questionHelpText,
        validationPredicates);
  }

  public NameQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(
        version,
        name,
        path,
        description,
        lifecycleStage,
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
    return getPath().join("first");
  }

  public ScalarType getFirstNameType() {
    return ScalarType.STRING;
  }

  public Path getMiddleNamePath() {
    return getPath().join("middle");
  }

  public ScalarType getMiddleNameType() {
    return ScalarType.STRING;
  }

  public Path getLastNamePath() {
    return getPath().join("last");
  }

  public ScalarType getLastNameType() {
    return ScalarType.STRING;
  }
}
