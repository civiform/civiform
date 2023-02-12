package services.geo;

import com.google.auto.value.AutoValue;

/** Represents the inclusion state of an address in a service area. */
@AutoValue
public abstract class ServiceAreaInclusion {
  public static Builder builder() {
    return new AutoValue_ServiceAreaInclusion.Builder();
  }

  /** The ID of the service area that was checked for inclusion. */
  public abstract String getServiceAreaId();

  /** The inclusion state of the service area. */
  public abstract ServiceAreaState getState();

  /** The timestamp for when the inclusion check was made. */
  public abstract long getTimeStamp();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setServiceAreaId(String id);

    public abstract Builder setState(ServiceAreaState state);

    public abstract Builder setTimeStamp(long now);

    public abstract ServiceAreaInclusion build();
  }
}
