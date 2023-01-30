package services.geo.esri;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
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
  public void getLabels() {
    Optional<ImmutableList<String>> maybeLabels = esriServiceAreaValidationConfig.getLabels();
    assertEquals(true, maybeLabels.isPresent());
    assertEquals("Seattle", maybeLabels.get().get(0));
  }

  @Test
  public void getValues() {
    Optional<ImmutableList<String>> maybeValues = esriServiceAreaValidationConfig.getValues();
    assertEquals(true, maybeValues.isPresent());
    assertEquals("Seattle", maybeValues.get().get(0));
  }

  @Test
  public void getUrls() {
    Optional<ImmutableList<String>> maybeUrls = esriServiceAreaValidationConfig.getUrls();
    assertEquals(true, maybeUrls.isPresent());
    assertEquals("/query", maybeUrls.get().get(0));
  }

  @Test
  public void getPaths() {
    Optional<ImmutableList<String>> maybePaths = esriServiceAreaValidationConfig.getPaths();
    assertEquals(true, maybePaths.isPresent());
    assertEquals("features[*].attributes.CITYNAME", maybePaths.get().get(0));
  }

  @Test
  public void getOptionByServiceArea() {
    Optional<EsriServiceAreaValidationOption> serviceAreaOption =
        esriServiceAreaValidationConfig.getOptionByServiceArea("Seattle");
    assertEquals(true, serviceAreaOption.isPresent());
    assertEquals("Seattle", serviceAreaOption.get().getLabel());
    assertEquals("Seattle", serviceAreaOption.get().getValue());
    assertEquals("/query", serviceAreaOption.get().getUrl());
    assertEquals("features[*].attributes.CITYNAME", serviceAreaOption.get().getPath());
  }
}
