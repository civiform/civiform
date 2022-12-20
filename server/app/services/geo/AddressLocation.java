package services.geo;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class AddressLocation {
  public static Builder builder() {
    return new AutoValue_AddressLocation.Builder();
  }

  public abstract Long x();

  public abstract Long y();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setX(long x);

    public abstract Builder setY(long y);
    
    public abstract AddressLocation build();
  }

}
