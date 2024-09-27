package services.geo.esri.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SpatialReference object from ESRI's findAddressCandidates results
 *
 * <p>@see <a
 * href="https://developers.arcgis.com/rest/geocode/api-reference/geocoding-find-address-candidates.htm">Find
 * Address Candidates</a>
 */
public final class SpatialReference {
  private final int wkid;
  private final int latestWkid;

  public SpatialReference(
      @JsonProperty("wkid") int wkid, @JsonProperty("latestWkid") int latestWkid) {
    this.wkid = wkid;
    this.latestWkid = latestWkid;
  }

  public int wkid() {
    return wkid;
  }

  public int latestWkid() {
    return latestWkid;
  }
}
