package services.geo.esri;

import com.google.auto.value.AutoValue;
import java.time.Instant;

@AutoValue
public abstract class EsriServiceAreaInclusion {
  public static Builder builder() {
    return new AutoValue_EsriServiceAreaInclusion.Builder();
  }

  public abstract String getArea();

  public abstract EsriServiceAreaState getState();

  public abstract Instant getTimeStamp();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setArea(String area);

    public abstract Builder setState(EsriServiceAreaState state);

    public abstract Builder setTimeStamp(Instant now);

    public abstract EsriServiceAreaInclusion build();
  }
}
