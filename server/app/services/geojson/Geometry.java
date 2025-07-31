package services.geojson;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Geometry(
    @JsonProperty(required = true) @JsonAlias({"Type", "type"}) String type,
    @JsonProperty(required = true) @JsonAlias({"Coordinates", "coordinates"})
        List<Double> coordinates) {
  @JsonCreator
  public Geometry {
    if (!type.equalsIgnoreCase("Point")) {
      throw new IllegalArgumentException("Invalid geometry type: " + type);
    }

    if (coordinates == null || coordinates.size() != 2) {
      throw new IllegalArgumentException("Coordinates must be an array of exactly two numbers.");
    }
  }
}
