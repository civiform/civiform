package services.question.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.OptionalInt;
import lombok.Getter;
import services.CiviFormError;
import services.question.QuestionSetting;

@Getter
public final class MapQuestionDefinition extends QuestionDefinition {
  @JsonProperty("questionSettings")
  private final ImmutableList<QuestionSetting> questionSettings;

  public MapQuestionDefinition(
      @JsonProperty("config") QuestionDefinitionConfig config,
      @JsonProperty("questionSettings") ImmutableList<QuestionSetting> questionSettings) {
    super(config);
    this.questionSettings = questionSettings;
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
    OptionalInt maxLocationSelections = getMapValidationPredicates().maxLocationSelections();

    if (geoJsonEndpoint.isEmpty()) {
      errors.add(CiviFormError.of("Map question must have valid GeoJSON"));
    }

    if (maxLocationSelections.isPresent()) {
      if (maxLocationSelections.getAsInt() < 1) {
        errors.add(CiviFormError.of("Max location selections cannot be less than 1"));
      }
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
        int maxLocationSelections, String geoJsonEndpoint) {
      return builder()
          .setMaxLocationSelections(maxLocationSelections)
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

    @JsonProperty("maxLocationSelections")
    public abstract OptionalInt maxLocationSelections();

    @JsonProperty("geoJsonEndpoint")
    public abstract String geoJsonEndpoint();

    @AutoValue.Builder
    public abstract static class Builder {

      @JsonProperty("maxLocationSelections")
      public abstract MapValidationPredicates.Builder setMaxLocationSelections(
          OptionalInt maxLocationSelections);

      public abstract MapValidationPredicates.Builder setMaxLocationSelections(
          int maxLocationSelections);

      @JsonProperty("geoJsonEndpoint")
      public abstract MapValidationPredicates.Builder setGeoJsonEndpoint(String geoJsonEndpoint);

      public abstract MapValidationPredicates build();
    }
  }
}
