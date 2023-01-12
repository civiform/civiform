package services.geo;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/**
 * Represents address candidates returned from Esri's findAddressCandidates
 *
 * <p>See {@link EsriClient} for details on getAddressCandidates
 */
@AutoValue
public abstract class AddressSuggestionGroup {
  public static Builder builder() {
    return new AutoValue_AddressSuggestionGroup.Builder();
  }

  /** returns a well-known ID for ArcGIS coordinate systems, used for spatial reference */
  public abstract int getWellKnownId();

  /** returns a list of address suggestions sorted by their score (best match) */
  public abstract ImmutableList<AddressSuggestion> getAddressSuggestionGroup();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setWellKnownId(int wellKnownId);

    public abstract Builder setAddressSuggestionGroup(ImmutableList<AddressSuggestion> suggestions);

    public abstract AddressSuggestionGroup build();
  }
}
