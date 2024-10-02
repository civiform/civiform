package services.geo;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import services.Address;

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

  public abstract Address getOriginalAddress();

  /** Returns an empty instance of an AddressSuggestionGroup */
  public static AddressSuggestionGroup empty(Address originalAddress) {
    return AddressSuggestionGroup.builder()
        .setWellKnownId(0)
        .setOriginalAddress(originalAddress)
        .setAddressSuggestions(ImmutableList.of())
        .build();
  }

  /**
   * returns a list of address suggestions ordered from highest scoring (most likely match) to
   * lowest scoring (least likely match).
   */
  public abstract ImmutableList<AddressSuggestion> getAddressSuggestions();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setWellKnownId(int wellKnownId);

    public abstract Builder setAddressSuggestions(ImmutableList<AddressSuggestion> suggestions);

    public abstract Builder setOriginalAddress(Address address);

    public abstract AddressSuggestionGroup build();
  }
}
