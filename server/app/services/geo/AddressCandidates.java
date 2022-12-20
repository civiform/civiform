package services.geo;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import services.geo.AddressCandidate;

@AutoValue
public abstract class AddressCandidates {
  public static Builder builder() {
    return new AutoValue_AddressCandidates.Builder();
  }

  public abstract int wkid();

  public abstract ImmutableList<AddressCandidate> candidates();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setWkid(int wkid);

    public abstract Builder setCandidates(ImmutableList<AddressCandidate> candidates);

    public abstract AddressCandidates build();
  }
}
