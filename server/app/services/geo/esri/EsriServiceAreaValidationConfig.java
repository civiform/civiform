package services.geo.esri;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import java.util.Optional;
import javax.inject.Inject;

/** Provides methods for handling Esri address service area validation config. */
public final class EsriServiceAreaValidationConfig {
  public Optional<ConfigList> ESRI_ADDRESS_SERVICE_AREA_VALIDATION_LABELS;
  public Optional<ConfigList> ESRI_ADDRESS_SERVICE_AREA_VALIDATION_VALUES;
  public Optional<ConfigList> ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS;
  public Optional<ConfigList> ESRI_ADDRESS_SERVICE_AREA_VALIDATION_PATHS;

  @Inject
  public EsriServiceAreaValidationConfig(Config configuration) {
    this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_LABELS =
        configuration.hasPath("esri_address_service_area_validation_labels")
            ? Optional.of(configuration.getList("esri_address_service_area_validation_labels"))
            : Optional.empty();
    this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_VALUES =
        configuration.hasPath("esri_address_service_area_validation_values")
            ? Optional.of(configuration.getList("esri_address_service_area_validation_values"))
            : Optional.empty();
    this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS =
        configuration.hasPath("esri_address_service_area_validation_urls")
            ? Optional.of(configuration.getList("esri_address_service_area_validation_urls"))
            : Optional.empty();
    this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_PATHS =
        configuration.hasPath("esri_address_service_area_validation_paths")
            ? Optional.of(configuration.getList("esri_address_service_area_validation_paths"))
            : Optional.empty();
  }

  public Boolean hasAllElements() {
    if (this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_LABELS.isEmpty()
        || this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_VALUES.isEmpty()
        || this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS.isEmpty()
        || this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_PATHS.isEmpty()) {
      return false;
    }

    return true;
  }

  private Optional<ImmutableList<String>> getConfigListSetting(Optional<ConfigList> setting) {
    if (setting.isEmpty()) {
      return Optional.empty();
    }

    ImmutableList.Builder<String> listBuilder = ImmutableList.builder();
    setting.get().stream()
        .forEach(configValue -> listBuilder.add((String) configValue.unwrapped()));
    return Optional.of(listBuilder.build());
  }

  public Optional<ImmutableList<String>> getLabels() {
    return getConfigListSetting(this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_LABELS);
  }

  public Optional<ImmutableList<String>> getValues() {
    return getConfigListSetting(this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_VALUES);
  }

  public Optional<ImmutableList<String>> getUrls() {
    return getConfigListSetting(this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS);
  }

  public Optional<ImmutableList<String>> getPaths() {
    return getConfigListSetting(this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_PATHS);
  }

  private Optional<ImmutableList<EsriServiceAreaValidationOption>> toImmutableList() {
    if (!hasAllElements()) {
      return Optional.empty();
    }

    ImmutableList<String> labels = getLabels().get();
    ImmutableList<String> values = getValues().get();
    ImmutableList<String> urls = getUrls().get();
    ImmutableList<String> paths = getPaths().get();

    ImmutableList.Builder<EsriServiceAreaValidationOption> listBuilder = ImmutableList.builder();

    for (int i = 0; i < values.size(); i++) {
      EsriServiceAreaValidationOption option =
          EsriServiceAreaValidationOption.builder()
              .setLabel(labels.get(i))
              .setValue(values.get(i))
              .setUrl(urls.get(i))
              .setPath(paths.get(i))
              .build();

      listBuilder.add(option);
    }

    return Optional.of(listBuilder.build());
  }

  public Optional<EsriServiceAreaValidationOption> getOptionByServiceArea(String serviceArea) {
    Optional<ImmutableList<EsriServiceAreaValidationOption>> list = toImmutableList();

    if (list.isEmpty()) {
      return Optional.empty();
    }

    int validationIndex = getValues().get().indexOf(serviceArea);
    return Optional.of(list.get().get(validationIndex));
  }
}
