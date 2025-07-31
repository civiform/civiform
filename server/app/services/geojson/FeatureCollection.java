package services.geojson;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * GeoJSON FeatureCollection object <a
 * href="https://datatracker.ietf.org/doc/html/rfc7946#section-3.3">See GeoJSON specs</a>
 *
 * @param type string for parsing incoming data - must be "FeatureCollection"
 * @param features a list of GeoJSON {@link services.geojson.Feature} objects
 */
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
