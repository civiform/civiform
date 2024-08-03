package services.geo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
@JsonDeserialize(builder = AutoValue_AddressSuggestion.Builder.class)
public abstract class AddressSuggestion {
  public static Builder builder() {
    return new AutoValue_AddressSuggestion.Builder();
  }

  /** returns an address as a single line */
  @JsonProperty("singleLineAddress")
  public abstract String getSingleLineAddress();

  /** Returns the location object, which conatiins x and y coordinates */
  @JsonProperty("location")
  public abstract AddressLocation getLocation();

  /**
   * Returns an integer which represents how well the candidate address matches with the user
   * inputted address
   *
   * <p>Score has the range [0-100], with 100 being the best possible score.
   */
  @JsonProperty("score")
  public abstract int getScore();

  /** returns address attributes like getStreet() */
  @JsonProperty("address")
  public abstract Address getAddress();

  @JsonProperty("correctionSource")
  public abstract String getCorrectionSource();

  @AutoValue.Builder
  public abstract static class Builder {
    @JsonProperty("singleLineAddress")
    public abstract Builder setSingleLineAddress(String singleLineAddress);

    @JsonProperty("location")
    public abstract Builder setLocation(AddressLocation location);

    @JsonProperty("score")
    public abstract Builder setScore(int score);

    @JsonProperty("address")
    public abstract Builder setAddress(Address address);

    @JsonProperty("correctionSource")
    public abstract Builder setCorrectionSource(String correctionSource);

    public abstract AddressSuggestion build();
  }
}
