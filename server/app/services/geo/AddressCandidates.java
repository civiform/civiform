package services.geo;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import services.geo.AddressCandidate;

@AutoValue
public abstract class AddressCandidates {
  public static Builder builder() {
    return new AutoValue_AddressCandidates.Builder();
  }

  /**
   * this method returns a well-known ID for ArcGIS coordinate systems, used for spatial reference
   * @return int
   */
  public abstract int getWkid();

  /**
   * this method returns a list of address candidates sorted by their score (best match)
   * @return a list of address cnadidates
   */
  public abstract ImmutableList<AddressCandidate> getCandidates();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setWkid(int wkid);

    public abstract Builder setCandidates(ImmutableList<AddressCandidate> candidates);

    public abstract AddressCandidates build();
  }
}
