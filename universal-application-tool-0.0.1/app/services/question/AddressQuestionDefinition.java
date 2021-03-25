package services.question;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalLong;
import services.Path;

public class AddressQuestionDefinition extends QuestionDefinition {

  public AddressQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      AddressValidationPredicates validationPredicates) {
    super(
        id, version, name, path, description, questionText, questionHelpText, validationPredicates);
  }

  public AddressQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      AddressValidationPredicates validationPredicates) {
    super(version, name, path, description, questionText, questionHelpText, validationPredicates);
  }

  public AddressQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(
        version,
        name,
        path,
        description,
        questionText,
        questionHelpText,
        AddressValidationPredicates.create());
  }

  @AutoValue
  public abstract static class AddressValidationPredicates extends ValidationPredicates {

    public static AddressValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_AddressQuestionDefinition_AddressValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static AddressValidationPredicates create() {
      return new AutoValue_AddressQuestionDefinition_AddressValidationPredicates();
    }
  }

  public AddressValidationPredicates getAddressValidationPredicates() {
    return (AddressValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ADDRESS;
  }

  @Override
  public ImmutableMap<Path, ScalarType> getScalarPaths() {
    return ImmutableMap.of(
        getStreetPath(), getStreetType(),
        getCityPath(), getCityType(),
        getStatePath(), getStateType(),
        getZipPath(), getZipType());
  }

  public Path getStreetPath() {
    return getPath().toBuilder().append("street").build();
  }

  public ScalarType getStreetType() {
    return ScalarType.STRING;
  }

  public Path getCityPath() {
    return getPath().toBuilder().append("city").build();
  }

  public ScalarType getCityType() {
    return ScalarType.STRING;
  }

  public Path getStatePath() {
    return getPath().toBuilder().append("state").build();
  }

  public ScalarType getStateType() {
    return ScalarType.STRING;
  }

  public Path getZipPath() {
    return getPath().toBuilder().append("zip").build();
  }

  public ScalarType getZipType() {
    return ScalarType.STRING;
  }
}
