package services.geo;

import com.google.auto.value.AutoValue;

/** Represents an address' location used by geo location services */
@AutoValue
public abstract class AddressLocation {
  public static Builder builder() {
    return new AutoValue_AddressLocation.Builder();
  }

  /** returns a coordinate on the x axis of the coordinate system (longitude) */
  public abstract Double getLongitude();

  /** returns a coordinate on the y axis of the coordinate system (latitude) */
  public abstract Double getLatitude();

  /** returns a well-known ID for ArcGIS coordinate systems, used for spatial reference */
  public abstract Integer getWellKnownId();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setLongitude(double x);

    public abstract Builder setLatitude(double y);

    public abstract Builder setWellKnownId(int wellKnownId);

    public abstract AddressLocation build();
  }
}
