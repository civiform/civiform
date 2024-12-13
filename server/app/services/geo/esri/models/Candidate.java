package services.geo.esri.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Candidate object from ESRI's findAddressCandidates results
 *
 * <p>@see <a
 * href="https://developers.arcgis.com/rest/geocode/api-reference/geocoding-find-address-candidates.htm">Find
 * Address Candidates</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Candidate {
  private final String address;
  private final Location location;
  private final int score;
  private final Attributes attributes;

  public Candidate(
      @JsonProperty("address") String address,
      @JsonProperty("location") Location location,
      @JsonProperty("score") int score,
      @JsonProperty("attributes") Attributes attributes) {
    this.address = address;
    this.location = location;
    this.score = score;
    this.attributes = attributes;
  }

  public String address() {
    return address;
  }

  public Location location() {
    return location;
  }

  public int score() {
    return score;
  }

  public Attributes attributes() {
    return attributes;
  }
}
