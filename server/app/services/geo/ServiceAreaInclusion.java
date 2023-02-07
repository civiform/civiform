package services.geo;

import com.google.auto.value.AutoValue;
import java.time.Instant;

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
  public abstract Instant getTimeStamp();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setServiceAreaId(String id);

    public abstract Builder setState(ServiceAreaState state);

    public abstract Builder setTimeStamp(Instant now);

    public abstract ServiceAreaInclusion build();
  }
}
