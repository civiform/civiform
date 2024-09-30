package services.geo.esri.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Location object from ESRI's findAddressCandidates results
 *
 * <p>@see <a
 * href="https://developers.arcgis.com/rest/geocode/api-reference/geocoding-find-address-candidates.htm">Find
 * Address Candidates</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Location {
  private final double x;
  private final double y;

  public Location(@JsonProperty("x") double x, @JsonProperty("y") double y) {
    this.x = x;
    this.y = y;
  }

  public double x() {
    return x;
  }

  public double y() {
    return y;
  }
}
