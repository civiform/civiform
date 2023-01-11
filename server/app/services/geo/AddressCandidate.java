package services.geo;

import com.google.auto.value.AutoValue;

/** This class is for setting and getting an address candidate */
@AutoValue
public abstract class AddressCandidate {
  public static Builder builder() {
    return new AutoValue_AddressCandidate.Builder();
  }
  /**
   * this method returns an address as a single line
   *
   * @return String.
   */
  public abstract String getAddress();

  /**
   * this method returns the location object, which conatiins x and y coordinates
   *
   * @return AddressLocation
   */
  public abstract AddressLocation getLocation();

  /**
   * this method returns an integer which represents how well the candidate address matches with the
   * user inputted address
   *
   * @return int
   */
  public abstract int getScore();

  /**
   * this method returns address attributes like getStreetValue()
   *
   * @return AddressAttributes
   */
  public abstract AddressAttributes getAttributes();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setAddress(String address);

    public abstract Builder setLocation(AddressLocation location);

    public abstract Builder setScore(int score);

    public abstract Builder setAttributes(AddressAttributes attributes);

    public abstract AddressCandidate build();
  }
}
