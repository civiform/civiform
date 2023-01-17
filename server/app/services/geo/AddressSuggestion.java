package services.geo;

import com.google.auto.value.AutoValue;
import services.Address;

/**
 * Represents an address suggestion. An address suggestion is provided by a geolocation service by
 * closest match to a user inputted address. Suggestions also contain geo coordinates which are
 * necessary for validating that an address is within a service area.
 *
 * <p>See {@link AddressSuggestionGroup}
 */
@AutoValue
public abstract class AddressSuggestion {
  public static Builder builder() {
    return new AutoValue_AddressSuggestion.Builder();
  }
  /** returns an address as a single line */
  public abstract String getSingleLineAddress();

  /** Returns the location object, which conatiins x and y coordinates */
  public abstract AddressLocation getLocation();

  /**
   * Returns an integer which represents how well the candidate address matches with the user
   * inputted address
   *
   * <p>Score has the range [0-100], with 100 being the best possible score.
   */
  public abstract int getScore();

  /** returns address attributes like getStreet() */
  public abstract Address getAddress();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setSingleLineAddress(String singleLineAddress);

    public abstract Builder setLocation(AddressLocation location);

    public abstract Builder setScore(int score);

    public abstract Builder setAddress(Address address);

    public abstract AddressSuggestion build();
  }
}
