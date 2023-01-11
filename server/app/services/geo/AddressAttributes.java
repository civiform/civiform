package services.geo;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class AddressAttributes {
  public static Builder builder() {
    return new AutoValue_AddressAttributes.Builder();
  }

  public abstract String getStreetValue();

  public abstract String getLine2Value();

  public abstract String getCityValue();

  public abstract String getStateValue();

  public abstract String getZipValue();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setStreetValue(String address);

    public abstract Builder setLine2Value(String subAddress);

    public abstract Builder setCityValue(String city);

    public abstract Builder setStateValue(String region);

    public abstract Builder setZipValue(String postal);

    public abstract AddressAttributes build();
  }
}
