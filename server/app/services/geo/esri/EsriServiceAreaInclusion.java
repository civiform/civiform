package services.geo.esri;

import com.google.auto.value.AutoValue;
import java.time.Instant;

/** Represents a service area and its inclusion state for an address location. */
@AutoValue
public abstract class EsriServiceAreaInclusion {
  public static Builder builder() {
    return new AutoValue_EsriServiceAreaInclusion.Builder();
  }

  /** returns the service area that was checked for inclusion. */
  public abstract String getArea();

  /** returns the inclusion state of the service area. */
  public abstract EsriServiceAreaState getState();

  /** return the timestamp for when the inclusion check was made. */
  public abstract Instant getTimeStamp();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setArea(String area);

    public abstract Builder setState(EsriServiceAreaState state);

    public abstract Builder setTimeStamp(Instant now);

    public abstract EsriServiceAreaInclusion build();
  }
}
