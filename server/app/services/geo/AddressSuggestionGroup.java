package services.geo;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/**
 * Represents address suggestions returned from Esri's findAddressCandidates endpoint
 *
 * <p>See {@link AddressSuggestion} for details on address suggestions
 */
@AutoValue
public abstract class AddressSuggestionGroup {
  public static Builder builder() {
    return new AutoValue_AddressSuggestionGroup.Builder();
  }

  /** returns a well-known ID for ArcGIS coordinate systems, used for spatial reference */
  public abstract int getWellKnownId();

  /**
   * returns a list of address suggestions ordered from highest scoring (most likely match) to
   * lowest scoring (least likely match).
   */
  public abstract ImmutableList<AddressSuggestion> getAddressSuggestions();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setWellKnownId(int wellKnownId);

    public abstract Builder setAddressSuggestions(ImmutableList<AddressSuggestion> suggestions);

    public abstract AddressSuggestionGroup build();
  }
}
