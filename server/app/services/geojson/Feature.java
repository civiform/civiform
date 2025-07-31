package services.geojson;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * GeoJSON Feature object <a href="https://datatracker.ietf.org/doc/html/rfc7946#section-3.2">See GeoJSON specs</a>
 * @param type
 * @param geometry
 * @param properties
 * @param id
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Feature(
    @JsonProperty(required = true) @JsonAlias({"Type", "type"}) String type,
    @JsonProperty(required = true) @JsonAlias({"Geometry", "geometry"}) Geometry geometry,
    @JsonProperty(required = true) @JsonAlias({"Properties", "properties"})
        Map<String, String> properties,
    @JsonProperty(required = true) @JsonAlias({"Id", "id"}) String id) {
  @JsonCreator
  public Feature {
    if (!type.equalsIgnoreCase("Feature")) {
      throw new IllegalArgumentException("Invalid type for Feature: " + type);
    }
  }
}
