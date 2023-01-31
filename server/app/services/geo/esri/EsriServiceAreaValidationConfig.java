package services.geo.esri;

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
  private static final String CONFIG_ERROR_STRING =
      "Esri address service area validation config not added properly.";

  private Optional<ConfigList> ESRI_ADDRESS_SERVICE_AREA_VALIDATION_LABELS;
  private Optional<ConfigList> ESRI_ADDRESS_SERVICE_AREA_VALIDATION_IDS;
  private Optional<ConfigList> ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS;
  private Optional<ConfigList> ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ATTRIBUTES;

  private Optional<ImmutableMap<String, EsriServiceAreaValidationOption>>
      esriServiceAreaValidationMap;

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
        return false;
      }
    }
  }

  private Optional<ImmutableList<String>> getConfigListSetting(Optional<ConfigList> setting) {
    if (setting.isEmpty()) {
      logger.error(CONFIG_ERROR_STRING);
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
      logger.error(CONFIG_ERROR_STRING);
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
    return Optional.of(mapBuilder.build());
  }

  /**
   * Returns an {@link EsriServiceAreaValidationOption} from the config list for Esri address
   * service area settings given a service area id.
   */
  public Optional<EsriServiceAreaValidationOption> getOptionByServiceAreaId(String serviceAreaId) {
    Optional<ImmutableMap<String, EsriServiceAreaValidationOption>> options = getImmutableMap();

    if (options.isEmpty()) {
      logger.error(CONFIG_ERROR_STRING);
      return Optional.empty();
    }

    EsriServiceAreaValidationOption option = options.get().get(serviceAreaId);

    if (option == null) {
      logger.error(
          "Esri Service Area Validation Config Error: No service area specified for {}",
          serviceAreaId);
      return Optional.empty();
    }

    return Optional.of(option);
  }
}
