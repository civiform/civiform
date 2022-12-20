package services.geo;

import com.google.auto.value.AutoValue;
import services.geo.AddressLocation;
import services.geo.AddressAttributes;

@AutoValue
public abstract class AddressCandidate {
  public static Builder builder() {
    return new AutoValue_AddressCandidate.Builder();
  }

  public abstract String address();

  public abstract AddressLocation location();

  public abstract int score();

  public abstract AddressAttributes attributes();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setAddress(String address);

    public abstract Builder setLocation(AddressLocation location);

    public abstract Builder setScore(int score);

    public abstract Builder setAttributes(AddressAttributes attributes);

    public abstract AddressCandidate build();
  }
}
