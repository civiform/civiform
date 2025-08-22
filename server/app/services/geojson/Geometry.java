package services.geojson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * GeoJSON Geometry object <a href="https://datatracker.ietf.org/doc/html/rfc7946#section-3.1">See
 * GeoJSON specs</a>
 *
 * @param type string for parsing incoming data - must be "Point"
 * @param coordinates an array of exactly two numbers
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Geometry(
    @JsonProperty(value = "type", required = true) String type,
    @JsonProperty(value = "coordinates", required = true) List<Double> coordinates) {
  @JsonCreator
  public Geometry {
    if (!type.equalsIgnoreCase("Point")) {
      throw new IllegalArgumentException("Invalid type for Geometry: " + type);
    }

    if (coordinates == null || coordinates.size() != 2) {
      throw new IllegalArgumentException("Coordinates must be an array of exactly two numbers");
    }
  }
}
