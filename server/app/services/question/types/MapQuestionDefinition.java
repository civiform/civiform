package services.question.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.OptionalInt;
import services.CiviFormError;

public final class MapQuestionDefinition extends QuestionDefinition {
  public MapQuestionDefinition(@JsonProperty("config") QuestionDefinitionConfig config) {
    super(config);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.MAP;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return MapValidationPredicates.builder().setGeoJsonEndpoint("").build();
  }

  @Override
  ImmutableSet<CiviFormError> internalValidate(Optional<QuestionDefinition> previousDefinition) {
    ImmutableSet.Builder<CiviFormError> errors = new ImmutableSet.Builder<>();
    String geoJsonEndpoint = getMapValidationPredicates().geoJsonEndpoint();
    OptionalInt minChoicesRequired = getMapValidationPredicates().minChoicesRequired();
    OptionalInt maxChoicesAllowed = getMapValidationPredicates().maxChoicesAllowed();
    if (geoJsonEndpoint.isEmpty()) {
      errors.add(CiviFormError.of("Map question must have a GeoJSON endpoint"));
    }

    if (minChoicesRequired.isPresent()) {
      if (minChoicesRequired.getAsInt() < 0) {
        errors.add(CiviFormError.of("Minimum number of choices required cannot be negative"));
      }
    }

    if (maxChoicesAllowed.isPresent()) {
      if (maxChoicesAllowed.getAsInt() < 1) {
        errors.add(CiviFormError.of("Maximum number of choices allowed cannot be less than 1"));
      }
    }

    if (minChoicesRequired.isPresent()
        && maxChoicesAllowed.isPresent()
        && minChoicesRequired.getAsInt() > maxChoicesAllowed.getAsInt()) {
      errors.add(
          CiviFormError.of(
              "Minimum number of choices required must be less than or equal to the maximum choices"
                  + " allowed"));
    }

    return errors.build();
  }

  @JsonIgnore
  public MapValidationPredicates getMapValidationPredicates() {
    return (MapValidationPredicates) getValidationPredicates();
  }

  @JsonDeserialize(builder = AutoValue_MapQuestionDefinition_MapValidationPredicates.Builder.class)
  @AutoValue
  public abstract static class MapValidationPredicates extends ValidationPredicates {

    public static MapValidationPredicates create() {
      return builder().build();
    }

    public static MapValidationPredicates create(
        int minChoicesRequired, int maxChoicesAllowed, String geoJsonEndpoint) {
      return builder()
          .setMinChoicesRequired(minChoicesRequired)
          .setMaxChoicesAllowed(maxChoicesAllowed)
          .setGeoJsonEndpoint(geoJsonEndpoint)
          .build();
    }

    public static MapValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_MapQuestionDefinition_MapValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static MapValidationPredicates.Builder builder() {
      return new AutoValue_MapQuestionDefinition_MapValidationPredicates.Builder();
    }

    @JsonProperty("minChoicesRequired")
    public abstract OptionalInt minChoicesRequired();

    @JsonProperty("maxChoicesAllowed")
    public abstract OptionalInt maxChoicesAllowed();

    @JsonProperty("geoJsonEndpoint")
    public abstract String geoJsonEndpoint();

    @AutoValue.Builder
    public abstract static class Builder {

      @JsonProperty("minChoicesRequired")
      public abstract MapValidationPredicates.Builder setMinChoicesRequired(
          OptionalInt minChoicesRequired);

      public abstract MapValidationPredicates.Builder setMinChoicesRequired(int minChoicesRequired);

      @JsonProperty("maxChoicesAllowed")
      public abstract MapValidationPredicates.Builder setMaxChoicesAllowed(
          OptionalInt maxChoicesAllowed);

      public abstract MapValidationPredicates.Builder setMaxChoicesAllowed(int maxChoicesAllowed);

      @JsonProperty("geoJsonEndpoint")
      public abstract MapValidationPredicates.Builder setGeoJsonEndpoint(String geoJsonEndpoint);

      public abstract MapValidationPredicates build();
    }
  }
}
