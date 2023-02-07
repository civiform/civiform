package services.geo.esri;

import com.google.auto.value.AutoValue;
import java.time.Instant;

/** Represents the inclusion state of an address in a service area. */
@AutoValue
public abstract class EsriServiceAreaInclusion {
  public static Builder builder() {
    return new AutoValue_EsriServiceAreaInclusion.Builder();
  }

  /** The ID of the service area that was checked for inclusion. */
  public abstract String getServiceAreaId();

  /** The inclusion state of the service area. */
  public abstract EsriServiceAreaState getState();

  /** The timestamp for when the inclusion check was made. */
  public abstract Instant getTimeStamp();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setServiceAreaId(String id);

    public abstract Builder setState(EsriServiceAreaState state);

    public abstract Builder setTimeStamp(Instant now);

    public abstract EsriServiceAreaInclusion build();
  }
}
