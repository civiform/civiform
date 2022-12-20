package services.geo;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class AddressAttributes {
  public static Builder builder() {
    return new AutoValue_AddressAttributes.Builder();
  }

  public abstract String address();

  public abstract String subAddress();

  public abstract String city();

  public abstract String region();

  public abstract String postal();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setAddress(String address);

    public abstract Builder setSubAddress(String subAddress);

    public abstract Builder setCity(String city);

    public abstract Builder setRegion(String region);

    public abstract Builder setPostal(String postal);

    public abstract AddressAttributes build();
  }
}
