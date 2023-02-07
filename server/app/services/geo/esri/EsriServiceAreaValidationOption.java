package services.geo.esri;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import services.geo.ServiceAreaInclusion;

/**
 * Represents a service area defined in an external Esri system that {@link Address} can be checked
 * for membership in.
 */
@AutoValue
public abstract class EsriServiceAreaValidationOption {
  public static Builder builder() {
    return new AutoValue_EsriServiceAreaValidationOption.Builder();
  }

  /** Returns a human readable label used for display in the UI. */
  public abstract String getLabel();

  /** Returns an ID. Used in conjunction with attribute for service area validation. */
  public abstract String getId();

  /** Returns a URL to call an external Esri boundary layer service. */
  public abstract String getUrl();

  /**
   * Returns the attribute key to access on Esri feature attributes. Used in conjunction with
   * service area id for service area validation.
   *
   * <p>@see <a
   * href="https://developers.arcgis.com/rest/services-reference/enterprise/feature-feature-service-.htm">Esri
   * Features</a>
   *
   * @see <a href="https://github.com/json-path/JsonPath">JsonPath</a>
   */
  public abstract String getAttribute();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setLabel(String label);

    public abstract Builder setId(String value);

    public abstract Builder setUrl(String url);

    public abstract Builder setAttribute(String attribute);

    public abstract EsriServiceAreaValidationOption build();
  }

  /**
   * Determine if this EsriServiceAreaValidationOption is in the provided list of {@link
   * ServiceAreaInclusion}.
   *
   * @return boolean.
   */
  public Boolean isServiceAreaOptionInInclusionGroup(
      ImmutableList<ServiceAreaInclusion> inclusionGroup) {
    return inclusionGroup.stream()
        .map(ServiceAreaInclusion::getServiceAreaId)
        .collect(ImmutableList.toImmutableList())
        .contains(this.getId());
  }
}
