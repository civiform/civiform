package services.applicant.question;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

/**
 * Represents a map question selection with a feature ID and location name. Used for both
 * serializing selections to JSON and parsing them back.
 */
@AutoValue
@JsonDeserialize(builder = AutoValue_MapSelection.Builder.class)
public abstract class MapSelection {

  @JsonProperty("featureId")
  public abstract String featureId();

  @JsonProperty("locationName")
  public abstract String locationName();

  public static MapSelection create(String featureId, String locationName) {
    return builder().setFeatureId(featureId).setLocationName(locationName).build();
  }

  public static Builder builder() {
    return new AutoValue_MapSelection.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    @JsonProperty("featureId")
    public abstract Builder setFeatureId(String featureId);

    @JsonProperty("locationName")
    public abstract Builder setLocationName(String locationName);

    public abstract MapSelection build();
  }
}
