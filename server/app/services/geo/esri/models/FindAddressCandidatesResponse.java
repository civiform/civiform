package services.geo.esri.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;

/**
 * Container for root response object from ESRI's findAddressCandidates results
 *
 * <p>@see <a
 * href="https://developers.arcgis.com/rest/geocode/api-reference/geocoding-find-address-candidates.htm">Find
 * Address Candidates</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FindAddressCandidatesResponse {
  private final Optional<SpatialReference> spatialReference;
  private ImmutableList<Candidate> candidates;
  private final Optional<EsriError> error;

  public FindAddressCandidatesResponse(
      @JsonProperty("spatialReference") SpatialReference spatialReference,
      @JsonProperty("candidates") List<Candidate> candidates,
      @JsonProperty("error") EsriError error) {
    this.spatialReference = Optional.ofNullable(spatialReference);
    this.candidates = candidates != null ? ImmutableList.copyOf(candidates) : ImmutableList.of();
    this.error = Optional.ofNullable(error);
  }

  public Optional<SpatialReference> spatialReference() {
    return spatialReference;
  }

  public ImmutableList<Candidate> candidates() {
    return candidates;
  }

  public Optional<EsriError> error() {
    return error;
  }

  public void addCandidates(ImmutableList<Candidate> newCandidates) {
    if (newCandidates == null) {
      return;
    }

    candidates =
        ImmutableList.<Candidate>builder().addAll(candidates).addAll(newCandidates).build();
  }
}
