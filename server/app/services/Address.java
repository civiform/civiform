package services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

/** Represents a basic address. */
@AutoValue
@JsonDeserialize(builder = AutoValue_Address.Builder.class)
public abstract class Address {
  public static Builder builder() {
    return new AutoValue_Address.Builder();
  }

  @JsonProperty("street")
  public abstract String getStreet();

  @JsonProperty("line2")
  public abstract String getLine2();

  @JsonProperty("city")
  public abstract String getCity();

  @JsonProperty("state")
  public abstract String getState();

  @JsonProperty("zip")
  public abstract String getZip();

  @JsonIgnore
  @Override
  public final String toString() {
    // Examples
    //   700 5th Ave, Seattle, WA, 98104
    //   700 5th Ave, Suite 123, Seattle, WA, 98104
    return String.format(
        "%s%s, %s, %s, %s",
        getStreet(), hasLine2() ? ", " + getLine2() : "", getCity(), getState(), getZip());
  }

  public Boolean hasLine2() {
    return !getLine2().isEmpty();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    @JsonProperty("street")
    public abstract Builder setStreet(String address);

    @JsonProperty("line2")
    public abstract Builder setLine2(String subAddress);

    @JsonProperty("city")
    public abstract Builder setCity(String city);

    @JsonProperty("state")
    public abstract Builder setState(String region);

    @JsonProperty("zip")
    public abstract Builder setZip(String postal);

    public abstract Address build();
  }
}
