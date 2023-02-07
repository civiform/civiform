package services.geo.esri;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides methods for handling Esri address service area validation config. */
public final class EsriServiceAreaValidationConfig {
  private Optional<ConfigList> ESRI_ADDRESS_SERVICE_AREA_VALIDATION_LABELS;
  private Optional<ConfigList> ESRI_ADDRESS_SERVICE_AREA_VALIDATION_IDS;
  private Optional<ConfigList> ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS;
  private Optional<ConfigList> ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ATTRIBUTES;

  @VisibleForTesting
  Optional<ImmutableMap<String, EsriServiceAreaValidationOption>> esriServiceAreaValidationMap;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  public EsriServiceAreaValidationConfig(Config configuration) {
    this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_LABELS =
        configuration.hasPath("esri_address_service_area_validation_labels")
            ? Optional.of(configuration.getList("esri_address_service_area_validation_labels"))
            : Optional.empty();
    this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_IDS =
        configuration.hasPath("esri_address_service_area_validation_ids")
            ? Optional.of(configuration.getList("esri_address_service_area_validation_ids"))
            : Optional.empty();
    this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS =
        configuration.hasPath("esri_address_service_area_validation_urls")
            ? Optional.of(configuration.getList("esri_address_service_area_validation_urls"))
            : Optional.empty();
    this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ATTRIBUTES =
        configuration.hasPath("esri_address_service_area_validation_attributes")
            ? Optional.of(configuration.getList("esri_address_service_area_validation_attributes"))
            : Optional.empty();
  }

  /** Checks if each element necessary for Esri address service area validation is present. */
  public Boolean hasAllElements() {
    // check if any are empty
    if (this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_LABELS.isEmpty()
        || this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_IDS.isEmpty()
        || this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS.isEmpty()
        || this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ATTRIBUTES.isEmpty()) {
      logger.error(
          "EsriServiceAreaValidationConfig Error: Esri Address Service Area Validation is missing"
              + " settings.");
      return false;
    } else {
      // check for same size
      if (this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_LABELS.get().size()
              == this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_IDS.get().size()
          && this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_IDS.get().size()
              == this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS.get().size()
          && this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS.get().size()
              == this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ATTRIBUTES.get().size()) {
        return true;
      } else {
        logger.error(
            "EsriServiceAreaValidationConfig Error: Esri Address Service Area Validation Config"
                + " expects settings of the same length.");
        return false;
      }
    }
  }

  private Optional<ImmutableList<String>> getConfigListSetting(Optional<ConfigList> setting) {
    if (setting.isEmpty()) {
      logger.error(
          "Error calling getConfigListSetting. Error: Config setting {}, is empty.", setting);
      return Optional.empty();
    }

    ImmutableList.Builder<String> listBuilder = ImmutableList.builder();
    setting.get().stream()
        .forEach(configValue -> listBuilder.add((String) configValue.unwrapped()));
    return Optional.of(listBuilder.build());
  }

  /**
   * Transforms the config settings for Esri address service area validation into an optional
   * immutable map with a service area ID as the key and {@link EsriServiceAreaValidationOption} as
   * the value.
   */
  public Optional<ImmutableMap<String, EsriServiceAreaValidationOption>> getImmutableMap() {
    if (this.esriServiceAreaValidationMap != null) {
      return this.esriServiceAreaValidationMap;
    }

    if (!hasAllElements()) {
      logger.error(
          "Error calling EsriServiceAreaValidationConfig.getImmutableMap. Error: Esri Address"
              + " Service Area Config is missing settings.");
      return Optional.empty();
    }

    ImmutableMap.Builder<String, EsriServiceAreaValidationOption> mapBuilder =
        ImmutableMap.builder();

    ImmutableList<String> labels =
        getConfigListSetting(this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_LABELS).get();
    ImmutableList<String> values =
        getConfigListSetting(this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_IDS).get();
    ImmutableList<String> urls =
        getConfigListSetting(this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS).get();
    ImmutableList<String> attributes =
        getConfigListSetting(this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ATTRIBUTES).get();

    for (int i = 0; i < values.size(); i++) {
      EsriServiceAreaValidationOption option =
          EsriServiceAreaValidationOption.builder()
              .setLabel(labels.get(i))
              .setId(values.get(i))
              .setUrl(urls.get(i))
              .setAttribute(attributes.get(i))
              .build();

      mapBuilder.put(values.get(i), option);
    }

    return esriServiceAreaValidationMap = Optional.of(mapBuilder.build());
  }

  /**
   * Creates a list of {@link EsriServiceAreaValidationOption}s with the same URL as the passed in
   * EsriServiceAreaValidationOption, given a map returned from {@link getImmutableMap}.
   */
  public ImmutableList<EsriServiceAreaValidationOption> mapToListWithSameServiceAreaOptionUrl(
      EsriServiceAreaValidationOption serviceAreaOption,
      ImmutableMap<String, EsriServiceAreaValidationOption> immutableMap) {
    return immutableMap.values().stream()
        .filter(option -> option.getUrl().equals(serviceAreaOption.getUrl()))
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Returns an {@link EsriServiceAreaValidationOption} from the config list for Esri address
   * service area settings given a service area id.
   */
  public Optional<EsriServiceAreaValidationOption> getOptionByServiceAreaId(String serviceAreaId) {
    Optional<ImmutableMap<String, EsriServiceAreaValidationOption>> options = getImmutableMap();

    if (options.isEmpty()) {
      logger.error(
          "Error calling EsriServiceAreaValidationConfig.getOptionByServiceAreaId. Error:"
              + " EsriServiceAreaValidationConfig.getImmutableMap() returned empty.");
      return Optional.empty();
    }

    EsriServiceAreaValidationOption option = options.get().get(serviceAreaId);

    if (option == null) {
      logger.error(
          "Error calling EsriServiceAreaValidationConfig.getOptionByServiceAreaId. Error: No"
              + " service area specified for {}",
          serviceAreaId);
      return Optional.empty();
    }

    return Optional.of(option);
  }
}
