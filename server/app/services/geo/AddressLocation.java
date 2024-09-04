package services.geo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

/** Represents an address' location used by geo location services */
@AutoValue
@JsonDeserialize(builder = AutoValue_AddressLocation.Builder.class)
public abstract class AddressLocation {
  public static Builder builder() {
    return new AutoValue_AddressLocation.Builder();
  }

  /** returns a coordinate on the x axis of the coordinate system (longitude) */
  @JsonProperty("longitude")
  public abstract double getLongitude();

  /** returns a coordinate on the y axis of the coordinate system (latitude) */
  @JsonProperty("latitude")
  public abstract double getLatitude();

  /** returns a well-known ID for ArcGIS coordinate systems, used for spatial reference */
  @JsonProperty("wellKnownId")
  public abstract int getWellKnownId();

  @AutoValue.Builder
  public abstract static class Builder {
    @JsonProperty("longitude")
    public abstract Builder setLongitude(double x);

    @JsonProperty("latitude")
    public abstract Builder setLatitude(double y);

    @JsonProperty("wellKnownId")
    public abstract Builder setWellKnownId(int wellKnownId);

    public abstract AddressLocation build();
  }
}
