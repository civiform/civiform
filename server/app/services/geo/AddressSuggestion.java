package services.geo;

import com.google.auto.value.AutoValue;
import services.Address;

/**
 * Represents an address candidate in the context of address candidates
 *
 * <p>See {@link AddressCandidates} for details on address candidates
 */
@AutoValue
public abstract class AddressSuggestion {
  public static Builder builder() {
    return new AutoValue_AddressSuggestion.Builder();
  }
  /** returns an address as a single line */
  public abstract String getSingleLineAddress();

  /** returns the location object, which conatiins x and y coordinates */
  public abstract AddressLocation getLocation();

  /**
   * returns an integer which represents how well the candidate address matches with the user
   * inputted address
   *
   * <p>score has the range [0-100] sorted in descending order
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
