package services.geojson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GeoJSON FeatureCollection object <a
 * href="https://datatracker.ietf.org/doc/html/rfc7946#section-3.3">See GeoJSON specs</a>
 *
 * @param type string for parsing incoming data - must be "FeatureCollection"
 * @param features a list of GeoJSON {@link services.geojson.Feature} objects
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FeatureCollection(
    @JsonProperty(value = "type", required = true) String type,
    @JsonProperty(value = "features", required = true) List<Feature> features) {
  @JsonCreator
  public FeatureCollection {
    if (!type.equalsIgnoreCase("FeatureCollection")) {
      throw new IllegalArgumentException("Invalid type for FeatureCollection: " + type);
    }

    if (features == null || features.isEmpty()) {
      throw new IllegalArgumentException("FeatureCollection must contain at least one feature");
    }
  }

  /**
   * Extracts all unique property keys from all features in this collection.
   *
   * @return set of all possible property keys found across all features
   */
  public Set<String> getPossibleKeys() {
    Set<String> keys = new HashSet<>();
    features().forEach(feature -> keys.addAll(feature.properties().keySet()));
    return keys;
  }
}
