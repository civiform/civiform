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
import services.question.MapSettingType;
import services.question.QuestionSetting;

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
    OptionalInt maxLocationSelections = getMapValidationPredicates().maxLocationSelections();

    if (geoJsonEndpoint.isEmpty()) {
      errors.add(CiviFormError.of("Map question must have valid GeoJSON"));
    }

    if (maxLocationSelections.isPresent()) {
      if (maxLocationSelections.getAsInt() < 1) {
        errors.add(CiviFormError.of("Maximum location selections cannot be less than 1"));
      }
    }

    // Validate settings in a single pass
    ImmutableSet<QuestionSetting> settings = getQuestionSettings().orElse(ImmutableSet.of());
    boolean hasNameKey = false;
    boolean hasAddressKey = false;
    boolean hasDetailsUrlKey = false;
    int filterCount = 0;

    for (QuestionSetting setting : settings) {
      boolean isValidSetting = setting.settingKey() != null && !setting.settingKey().isEmpty();

      switch ((MapSettingType) setting.settingType()) {
        case LOCATION_NAME_GEO_JSON_KEY -> {
          if (isValidSetting) hasNameKey = true;
        }
        case LOCATION_ADDRESS_GEO_JSON_KEY -> {
          if (isValidSetting) hasAddressKey = true;
        }
        case LOCATION_DETAILS_URL_GEO_JSON_KEY -> {
          if (isValidSetting) hasDetailsUrlKey = true;
        }
        case LOCATION_TAG_GEO_JSON_KEY -> {
          if (!isValidSetting) {
            errors.add(CiviFormError.of("Tag key cannot be empty"));
          }
          if (setting.localizedSettingDisplayName().isEmpty()
              || setting.localizedSettingDisplayName().get().getDefault().isEmpty()) {
            errors.add(CiviFormError.of("Tag display name cannot be empty"));
          }
        }
        case LOCATION_FILTER_GEO_JSON_KEY -> {
          filterCount++;
          if (!isValidSetting) {
            errors.add(CiviFormError.of("Filter key cannot be empty"));
          }
          if (setting.localizedSettingDisplayName().isEmpty()
              || setting.localizedSettingDisplayName().get().getDefault().isEmpty()) {
            errors.add(CiviFormError.of("Filter display name cannot be empty"));
          }
        }
      }
    }

    if (!hasNameKey) {
      errors.add(CiviFormError.of("Name key cannot be empty"));
    }
    if (!hasAddressKey) {
      errors.add(CiviFormError.of("Address key cannot be empty"));
    }
    if (!hasDetailsUrlKey) {
      errors.add(CiviFormError.of("View more details URL key cannot be empty"));
    }
    if (filterCount > 6) {
      errors.add(CiviFormError.of("Question cannot have more than six filters"));
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
