package services.geojson;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FeatureCollection(
    @JsonProperty(required = true) @JsonAlias({"Type", "type"}) String type,
    @JsonProperty(required = true) @JsonAlias({"Features", "features"}) List<Feature> features) {
  @JsonCreator
  public FeatureCollection {
    if (!type.equalsIgnoreCase("FeatureCollection")) {
      throw new IllegalArgumentException("Invalid type for FeatureCollection: " + type);
    }

    if (features == null || features.isEmpty()) {
      throw new IllegalArgumentException("FeatureCollection must contain at least one feature.");
    }
  }
}
