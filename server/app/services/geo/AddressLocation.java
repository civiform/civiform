package services.geo;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class AddressLocation {
  public static Builder builder() {
    return new AutoValue_AddressLocation.Builder();
  }
  // TODO: consistent nameing long/lat or x/y?
  /**
   * this method returns a coordinate on the x axis of the coordinate system (longitude)
   * @return Long
   */
  public abstract Long getX();

  /**
   * this method returns a coordinate on the y axis of the coordinate system (latitude)
   * @return Long
   */
  public abstract Long getY();

  /**
   * this method returns a well-known ID for ArcGIS coordinate systems, used for spatial reference
   * @return int
   */
  public abstract int getWkid();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setX(long x);

    public abstract Builder setY(long y);

    public abstract Builder setWkid(int wkid);
    
    public abstract AddressLocation build();
  }

}
