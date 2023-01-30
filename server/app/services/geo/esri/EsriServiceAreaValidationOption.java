package services.geo.esri;

import com.google.auto.value.AutoValue;

/** Represents a service area to validate against. */
@AutoValue
public abstract class EsriServiceAreaValidationOption {
  public static Builder builder() {
    return new AutoValue_EsriServiceAreaValidationOption.Builder();
  }

  /** Returns a human readable label used for display in the UI. */
  public abstract String getLabel();

  /** Returns a value. Used in conjunction with path for service area validation. */
  public abstract String getValue();

  /** Returns a URL to call an external Esri boundary layer service. */
  public abstract String getUrl();

  /** Returns a JsonPath. Used in conjunction with value for service area validation. */
  public abstract String getPath();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setLabel(String label);

    public abstract Builder setValue(String value);

    public abstract Builder setUrl(String url);

    public abstract Builder setPath(String path);

    public abstract EsriServiceAreaValidationOption build();
  }
}
