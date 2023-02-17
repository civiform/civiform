package services.geo;

import com.fasterxml.jackson.databind.JsonNode;
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

    public AddressSuggestion fromJson(JsonNode node) {
      JsonNode location = node.get("location");
      JsonNode attributes = node.get("address");

      AddressLocation addressLocation =
          AddressLocation.builder()
              .setLatitude(location.get("latitude").asDouble())
              .setLongitude(location.get("longitude").asDouble())
              .setWellKnownId(location.get("wellKnownId").asInt())
              .build();

      Address candidateAddress =
          Address.builder()
              .setStreet(attributes.get("street").asText())
              .setLine2(attributes.get("line2").asText())
              .setCity(attributes.get("city").asText())
              .setState(attributes.get("state").asText())
              .setZip(attributes.get("zip").asText())
              .build();

      return AddressSuggestion.builder()
          .setSingleLineAddress(node.get("singleLineAddress").asText())
          .setScore(node.get("score").asInt())
          .setAddress(candidateAddress)
          .setLocation(addressLocation)
          .build();
    }
  }
}
