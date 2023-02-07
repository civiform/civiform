package services.geo.esri;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class EsriServiceAreaValidationConfigTest {
  private Config config;
  private EsriServiceAreaValidationConfig esriServiceAreaValidationConfig;

  @Before
  // setup config and esriServiceAreaValidationConfig
  public void setup() {
    config = ConfigFactory.load();
    esriServiceAreaValidationConfig = new EsriServiceAreaValidationConfig(config);
  }

  @Test
  public void hasAllElements() {
    assertEquals(true, esriServiceAreaValidationConfig.hasAllElements());
  }

  @Test
  public void getImmutableMap() {
    Optional<ImmutableMap<String, EsriServiceAreaValidationOption>> maybeMap =
        esriServiceAreaValidationConfig.getImmutableMap();
    assertEquals(true, maybeMap.isPresent());
    EsriServiceAreaValidationOption option = maybeMap.get().get("Seattle");
    assertEquals("Seattle", option.getLabel());
    assertEquals("Seattle", option.getId());
    assertEquals("/query", option.getUrl());
    assertEquals("CITYNAME", option.getAttribute());
  }

  @Test
  public void getImmutableMapStored() {
    assertEquals(esriServiceAreaValidationConfig.esriServiceAreaValidationMap, null);
    esriServiceAreaValidationConfig.getImmutableMap();
    assertEquals(true, esriServiceAreaValidationConfig.esriServiceAreaValidationMap.isPresent());
  }

  @Test
  public void getOptionByServiceAreaId() {
    Optional<EsriServiceAreaValidationOption> serviceAreaOption =
        esriServiceAreaValidationConfig.getOptionByServiceAreaId("Seattle");
    assertEquals(true, serviceAreaOption.isPresent());
    assertEquals("Seattle", serviceAreaOption.get().getLabel());
    assertEquals("Seattle", serviceAreaOption.get().getId());
    assertEquals("/query", serviceAreaOption.get().getUrl());
    assertEquals("CITYNAME", serviceAreaOption.get().getAttribute());
  }

  @Test
  public void getOptionByServiceAreaIdDoesNotExist() {
    Optional<EsriServiceAreaValidationOption> serviceAreaOption =
        esriServiceAreaValidationConfig.getOptionByServiceAreaId("Mars");
    assertEquals(true, serviceAreaOption.isEmpty());
  }

  @Test
  public void mapToListWithSameServiceAreaOptionUrl() {
    Optional<ImmutableMap<String, EsriServiceAreaValidationOption>> maybeMap =
        esriServiceAreaValidationConfig.getImmutableMap();
    assertEquals(true, maybeMap.isPresent());
    EsriServiceAreaValidationOption option = maybeMap.get().get("Seattle");
    ImmutableList<EsriServiceAreaValidationOption> optionList = esriServiceAreaValidationConfig.mapToListWithSameServiceAreaOptionUrl(option, maybeMap.get());
    Optional<EsriServiceAreaValidationOption> maybeOptionFromList = optionList.stream().findFirst();
    assertThat(maybeOptionFromList.isPresent()).isTrue();
    EsriServiceAreaValidationOption optionFromList = maybeOptionFromList.get();
    assertEquals("Seattle", optionFromList.getLabel());
    assertEquals("Seattle", optionFromList.getId());
    assertEquals("/query", optionFromList.getUrl());
    assertEquals("CITYNAME", optionFromList.getAttribute());
  }
}
