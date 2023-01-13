package services;

import com.google.auto.value.AutoValue;

/** Represents a basic address. */
@AutoValue
public abstract class Address {
  public static Builder builder() {
    return new AutoValue_Address.Builder();
  }

  public abstract String getStreet();

  public abstract String getLine2();

  public abstract String getCity();

  public abstract String getState();

  public abstract String getZip();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setStreet(String address);

    public abstract Builder setLine2(String subAddress);

    public abstract Builder setCity(String city);

    public abstract Builder setState(String region);

    public abstract Builder setZip(String postal);

    public abstract Address build();
  }
}
