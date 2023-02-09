package services.geo.esri;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides methods for handling Esri address service area validation config. */
public final class EsriServiceAreaValidationConfig {
  private Optional<ImmutableList<String>> ESRI_ADDRESS_SERVICE_AREA_VALIDATION_LABELS;
  private Optional<ImmutableList<String>> ESRI_ADDRESS_SERVICE_AREA_VALIDATION_IDS;
  private Optional<ImmutableList<String>> ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS;
  private Optional<ImmutableList<String>> ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ATTRIBUTES;

  @VisibleForTesting
  ImmutableMap<String, EsriServiceAreaValidationOption> esriServiceAreaValidationMap;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  public EsriServiceAreaValidationConfig(Config configuration) {
    this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_LABELS =
        configuration.hasPath("esri_address_service_area_validation_labels")
            ? Optional.of(
                configuration.getList("esri_address_service_area_validation_labels").stream()
                    .map(configValue -> (String) configValue.unwrapped())
                    .collect(ImmutableList.toImmutableList()))
            : Optional.empty();
    this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_IDS =
        configuration.hasPath("esri_address_service_area_validation_ids")
            ? Optional.of(
                configuration.getList("esri_address_service_area_validation_ids").stream()
                    .map(configValue -> (String) configValue.unwrapped())
                    .collect(ImmutableList.toImmutableList()))
            : Optional.empty();
    this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS =
        configuration.hasPath("esri_address_service_area_validation_urls")
            ? Optional.of(
                configuration.getList("esri_address_service_area_validation_urls").stream()
                    .map(configValue -> (String) configValue.unwrapped())
                    .collect(ImmutableList.toImmutableList()))
            : Optional.empty();
    this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ATTRIBUTES =
        configuration.hasPath("esri_address_service_area_validation_attributes")
            ? Optional.of(
                configuration.getList("esri_address_service_area_validation_attributes").stream()
                    .map(configValue -> (String) configValue.unwrapped())
                    .collect(ImmutableList.toImmutableList()))
            : Optional.empty();
  }

  /** Checks if each element necessary for Esri address service area validation is present. */
  public Boolean isConfigurationValid() {
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

  /**
   * Transforms the config settings for Esri address service area validation into an optional
   * immutable map with a service area ID as the key and {@link EsriServiceAreaValidationOption} as
   * the value.
   */
  public ImmutableMap<String, EsriServiceAreaValidationOption> getImmutableMap() {
    if (this.esriServiceAreaValidationMap != null) {
      return this.esriServiceAreaValidationMap;
    }

    if (!isConfigurationValid()) {
      logger.error(
          "Error calling EsriServiceAreaValidationConfig.getImmutableMap. Error: Esri Address"
              + " Service Area Config is missing settings.");
      throw new InvalidEsriServiceAreaValidationConfigException(
          "Esri Service Area Validation Config is missing settings.");
    }

    ImmutableMap.Builder<String, EsriServiceAreaValidationOption> mapBuilder =
        ImmutableMap.builder();

    ImmutableList<String> ids = this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_IDS.get();

    for (int i = 0; i < ids.size(); i++) {
      EsriServiceAreaValidationOption option =
          EsriServiceAreaValidationOption.builder()
              .setLabel(this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_LABELS.get().get(i))
              .setId(ids.get(i))
              .setUrl(this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS.get().get(i))
              .setAttribute(this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ATTRIBUTES.get().get(i))
              .build();

      mapBuilder.put(ids.get(i), option);
    }

    return esriServiceAreaValidationMap = mapBuilder.build();
  }

  /**
   * Creates a list of {@link EsriServiceAreaValidationOption}s with the same URL as the passed in
   * EsriServiceAreaValidationOption, given a map returned from {@link getImmutableMap}.
   */
  public ImmutableList<EsriServiceAreaValidationOption> getOptionsWithSharedBackend(
      String serviceAreaUrl) {
    if (!isConfigurationValid()) {
      logger.error(
          "Error calling EsriServiceAreaValidationConfig.getOptionsWithSharedBackend. Error: Esri"
              + " Address Service Area Config is missing settings.");
      throw new InvalidEsriServiceAreaValidationConfigException(
          "Esri Service Area Validation Config is missing settings.");
    }

    ImmutableList.Builder<EsriServiceAreaValidationOption> listBuilder = ImmutableList.builder();

    ImmutableList<String> ids = this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_IDS.get();
    ImmutableList<String> urls = this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS.get();

    for (int i = 0; i < ids.size(); i++) {
      if (urls.get(i).equals(serviceAreaUrl)) {
        EsriServiceAreaValidationOption option =
            EsriServiceAreaValidationOption.builder()
                .setLabel(this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_LABELS.get().get(i))
                .setId(ids.get(i))
                .setUrl(urls.get(i))
                .setAttribute(this.ESRI_ADDRESS_SERVICE_AREA_VALIDATION_ATTRIBUTES.get().get(i))
                .build();

        listBuilder.add(option);
      }
    }

    return listBuilder.build();
  }

  /**
   * Returns an {@link EsriServiceAreaValidationOption} from the config list for Esri address
   * service area settings given a service area id.
   */
  public Optional<EsriServiceAreaValidationOption> getOptionByServiceAreaId(String serviceAreaId) {
    ImmutableMap<String, EsriServiceAreaValidationOption> options = getImmutableMap();

    EsriServiceAreaValidationOption option = options.get(serviceAreaId);

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
