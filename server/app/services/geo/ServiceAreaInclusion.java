package services.geo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

/** Represents the inclusion state of an address in a service area. */
@AutoValue
public abstract class ServiceAreaInclusion {
  @JsonCreator
  public static ServiceAreaInclusion create(
      @JsonProperty("service_area_id") String serviceAreaId,
      @JsonProperty("state") ServiceAreaState state,
      @JsonProperty("timestamp") long timestamp) {
    return builder()
        .setServiceAreaId(serviceAreaId)
        .setState(state)
        .setTimeStamp(timestamp)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_ServiceAreaInclusion.Builder();
  }

  /** The ID of the service area that was checked for inclusion. */
  @JsonProperty("service_area_id")
  public abstract String getServiceAreaId();

  /** The inclusion state of the service area. */
  @JsonProperty("state")
  public abstract ServiceAreaState getState();

  /** The timestamp for when the inclusion check was made. */
  @JsonProperty("timestamp")
  public abstract long getTimeStamp();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setServiceAreaId(String id);

    public abstract Builder setState(ServiceAreaState state);

    public abstract Builder setTimeStamp(long now);

    public abstract ServiceAreaInclusion build();
  }
}
