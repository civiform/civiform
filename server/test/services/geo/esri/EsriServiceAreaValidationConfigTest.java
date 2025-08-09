package services.geo.esri;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class EsriServiceAreaValidationConfigTest {
  private Config config;
  private EsriServiceAreaValidationConfig esriServiceAreaValidationConfig;

  @Before
  // setup config and esriServiceAreaValidationConfig
  public void setup() {
    config =
        ConfigFactory.parseMap(
            ImmutableMap.of(
                "esri_address_service_area_validation_urls", List.of("/query"),
                "esri_address_service_area_validation_labels", List.of("Seattle"),
                "esri_address_service_area_validation_ids", List.of("Seattle"),
                "esri_address_service_area_validation_attributes", List.of("CITYNAME")));

    esriServiceAreaValidationConfig = new EsriServiceAreaValidationConfig(config);
  }

  @Test
  public void isConfigurationValid() {
    assertThat(esriServiceAreaValidationConfig.isConfigurationValid()).isTrue();
  }

  @Test
  public void getImmutableMap() {
    ImmutableMap<String, EsriServiceAreaValidationOption> map =
        esriServiceAreaValidationConfig.getImmutableMap();
    EsriServiceAreaValidationOption option = map.get("Seattle");
    assertThat(option.getLabel()).isEqualTo("Seattle");
    assertThat(option.getId()).isEqualTo("Seattle");
    assertThat(option.getUrl()).isEqualTo("/query");
    assertThat(option.getAttribute()).isEqualTo("CITYNAME");
  }

  @Test
  public void getImmutableMap_withMultipleUniqueSettings() {
    esriServiceAreaValidationConfig =
        new EsriServiceAreaValidationConfig(
            ConfigFactory.parseMap(
                ImmutableMap.of(
                    "esri_address_service_area_validation_urls",
                        List.of("/query1", "/query2", "/query3"),
                    "esri_address_service_area_validation_labels",
                        List.of("label1", "label2", "label3"),
                    "esri_address_service_area_validation_ids", List.of("id1", "id2", "id3"),
                    "esri_address_service_area_validation_attributes",
                        List.of("attr1", "attr2", "attr3"))));

    ImmutableMap<String, EsriServiceAreaValidationOption> map =
        esriServiceAreaValidationConfig.getImmutableMap();

    EsriServiceAreaValidationOption option = map.get("id1");
    assertThat(option).isNotNull();
    assertThat(option.getUrl()).isEqualTo("/query1");
    assertThat(option.getLabel()).isEqualTo("label1");
    assertThat(option.getId()).isEqualTo("id1");
    assertThat(option.getAttribute()).isEqualTo("attr1");

    option = map.get("id2");
    assertThat(option).isNotNull();
    assertThat(option.getUrl()).isEqualTo("/query2");
    assertThat(option.getLabel()).isEqualTo("label2");
    assertThat(option.getId()).isEqualTo("id2");
    assertThat(option.getAttribute()).isEqualTo("attr2");

    option = map.get("id3");
    assertThat(option).isNotNull();
    assertThat(option.getUrl()).isEqualTo("/query3");
    assertThat(option.getLabel()).isEqualTo("label3");
    assertThat(option.getId()).isEqualTo("id3");
    assertThat(option.getAttribute()).isEqualTo("attr3");
  }

  @Test
  public void getImmutableMap_withMultipleSettingsAndDuplicateIds() {
    esriServiceAreaValidationConfig =
        new EsriServiceAreaValidationConfig(
            ConfigFactory.parseMap(
                ImmutableMap.of(
                    "esri_address_service_area_validation_urls",
                        List.of("/query1", "/query2", "/query3"),
                    "esri_address_service_area_validation_labels",
                        List.of("label1", "label2", "label3"),
                    // This has two ids of the same value "id1"
                    "esri_address_service_area_validation_ids", List.of("id1", "id2", "id1"),
                    "esri_address_service_area_validation_attributes",
                        List.of("attr1", "attr2", "attr3"))));

    // This fails because `getImmutableMap` is not trying to build a map with multiple entries
    ImmutableMap<String, EsriServiceAreaValidationOption> map =
        esriServiceAreaValidationConfig.getImmutableMap();

    EsriServiceAreaValidationOption option = map.get("id1");
    assertThat(option).isNotNull();
    assertThat(option.getUrl()).isEqualTo("/query1");
    assertThat(option.getLabel()).isEqualTo("label1");
    assertThat(option.getId()).isEqualTo("id1");
    assertThat(option.getAttribute()).isEqualTo("attr1");
  }

  @Test
  public void getImmutableMapStored() {
    assertThat(esriServiceAreaValidationConfig.esriServiceAreaValidationMap).isNull();
    esriServiceAreaValidationConfig.getImmutableMap();
    assertThat(
            esriServiceAreaValidationConfig
                .esriServiceAreaValidationMap
                .get("Seattle")
                .getAttribute())
        .isEqualTo("CITYNAME");
  }

  @Test
  public void getOptionsWithSharedBackend() {
    assertThat(esriServiceAreaValidationConfig.isConfigurationValid()).isTrue();
    ImmutableMap<String, EsriServiceAreaValidationOption> map =
        esriServiceAreaValidationConfig.getImmutableMap();
    EsriServiceAreaValidationOption option = map.get("Seattle");

    ImmutableList<EsriServiceAreaValidationOption> optionList =
        esriServiceAreaValidationConfig.getOptionsWithSharedBackend(option.getUrl());

    Optional<EsriServiceAreaValidationOption> maybeOptionFromList = optionList.stream().findFirst();
    assertThat(maybeOptionFromList.isPresent()).isTrue();
    EsriServiceAreaValidationOption optionFromList = maybeOptionFromList.get();
    assertThat(optionFromList.getLabel()).isEqualTo("Seattle");
    assertThat(optionFromList.getId()).isEqualTo("Seattle");
    assertThat(optionFromList.getUrl()).isEqualTo("/query");
    assertThat(optionFromList.getAttribute()).isEqualTo("CITYNAME");
  }

  @Test
  public void getOptionByServiceAreaId() {
    Optional<EsriServiceAreaValidationOption> serviceAreaOption =
        esriServiceAreaValidationConfig.getOptionByServiceAreaId("Seattle");
    assertThat(serviceAreaOption.isPresent()).isTrue();
    assertThat(serviceAreaOption.get().getLabel()).isEqualTo("Seattle");
    assertThat(serviceAreaOption.get().getId()).isEqualTo("Seattle");
    assertThat(serviceAreaOption.get().getUrl()).isEqualTo("/query");
    assertThat(serviceAreaOption.get().getAttribute()).isEqualTo("CITYNAME");
  }

  @Test
  public void getOptionByServiceAreaIdDoesNotExist() {
    Optional<EsriServiceAreaValidationOption> serviceAreaOption =
        esriServiceAreaValidationConfig.getOptionByServiceAreaId("Mars");
    assertThat(serviceAreaOption).isEmpty();
  }

  @Test
  public void getOptionByServiceAreaIds() {
    Optional<ImmutableList<EsriServiceAreaValidationOption>> serviceAreaOptions =
        esriServiceAreaValidationConfig.getOptionsByServiceAreaIds(
            ImmutableList.of("Seattle", "Bloomington"));
    assertThat(serviceAreaOptions.isPresent()).isTrue();
    assertThat(serviceAreaOptions.get().get(0).getLabel()).isEqualTo("Seattle");
    assertThat(serviceAreaOptions.get().get(0).getId()).isEqualTo("Seattle");
    assertThat(serviceAreaOptions.get().get(0).getUrl()).isEqualTo("/query");
    assertThat(serviceAreaOptions.get().get(0).getAttribute()).isEqualTo("CITYNAME");
  }
}
