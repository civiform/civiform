package services.geo.esri.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;

/**
 * Attributes object from ESRI's findAddressCandidates results
 *
 * <p>@see <a
 * href="https://developers.arcgis.com/rest/geocode/api-reference/geocoding-find-address-candidates.htm">Find
 * Address Candidates</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Attributes {
  private final String subAddr;
  private final String address;
  private final String city;
  private final String region;
  private final String regionAbbr;
  private final String postal;

  public Attributes(
      @JsonProperty("SubAddr") String subAddr,
      @JsonProperty("Address") String address,
      @JsonProperty("City") String city,
      @JsonProperty("Region") String region,
      @JsonProperty("RegionAbbr") String regionAbbr,
      @JsonProperty("Postal") String postal) {
    this.subAddr = subAddr;
    this.address = address;
    this.city = city;
    this.region = region;
    this.regionAbbr = regionAbbr;
    this.postal = postal;
  }

  public Optional<String> subAddr() {
    return Optional.ofNullable(subAddr);
  }

  public String address() {
    return address;
  }

  public String city() {
    return city;
  }

  public String postal() {
    return postal;
  }

  /**
   * Get the state abbreviation from the attributes JSON node. On Public ESRI this is under the
   * RegionAbbr field, but some custom implementations may have it under the Region field.
   */
  public String stateAbbreviation() {
    // Start with getting the value from the RegionAbbr, this is the default used by ESRI for
    // state abbreviations. Use if two characters.
    if (regionAbbr.length() == 2) {
      return regionAbbr;
    }

    // If regionAbbr is not two characters it is either empty or the full state name so pull
    // from the region value. We don't use this be default because some custom implementations
    // use region.
    return region;
  }
}
