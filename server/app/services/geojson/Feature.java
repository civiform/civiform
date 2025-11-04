package services.geojson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * GeoJSON Feature object <a href="https://datatracker.ietf.org/doc/html/rfc7946#section-3.2">See
 * GeoJSON specs</a>
 *
 * @param type string for parsing incoming data - must be "Feature"
 * @param geometry {@link services.geojson.Geometry}
 * @param properties a map of key/value String pairs with information about the Feature
 * @param id unique id of the Feature
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Feature(
    @JsonProperty(value = "type", required = true) String type,
    @JsonProperty(value = "geometry", required = true) Geometry geometry,
    @JsonProperty(value = "properties", required = true) Map<String, String> properties,
    @JsonProperty(value = "id", required = true) String id) {
  @JsonCreator
  public Feature {
    if (!type.equalsIgnoreCase("Feature")) {
      throw new IllegalArgumentException("Invalid type for Feature: " + type);
    }

    if (properties.size() < 3) {
      throw new IllegalArgumentException("Properties objects are required.");
    }
  }
}
