package services.geo.esri;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.Clock;
import java.time.ZoneId;
import org.junit.Before;
import org.junit.Test;
import services.geo.AddressLocation;
import services.geo.ServiceAreaInclusion;

public class EsriServiceAreaValidationOptionTest {
  private Config config;
  EsriServiceAreaValidationConfig esriServiceAreaValidationConfig;
  private EsriServiceAreaValidationOption esriServiceAreaValidationOption;
  private AddressLocation location;

  // setup for service area validation
  private FakeEsriClient client;
  private ImmutableList<ServiceAreaInclusion> inclusionGroup;

  @Before
  // setup Server to return mock data from JSON files
  public void setup() {
    Clock clock = Clock.system(ZoneId.of("America/Los_Angeles"));
    config = ConfigFactory.load();
    esriServiceAreaValidationConfig = new EsriServiceAreaValidationConfig(config);

    esriServiceAreaValidationOption =
        EsriServiceAreaValidationOption.builder()
            .setLabel("Seattle")
            .setId("Seattle")
            .setUrl("/query")
            .setAttribute("CITYNAME")
            .build();

    location =
        AddressLocation.builder()
            .setLongitude(-100.0)
            .setLatitude(100.0)
            .setWellKnownId(4326)
            .build();

    client = new FakeEsriClient(clock, esriServiceAreaValidationConfig);

    inclusionGroup =
        client
            .getServiceAreaInclusionGroup(esriServiceAreaValidationOption, location)
            .toCompletableFuture()
            .join();
  }

  @Test
  public void isServiceAreaOptionInInclusionGroup() {
    assertEquals(
        true, esriServiceAreaValidationOption.isServiceAreaOptionInInclusionGroup(inclusionGroup));
  }

  @Test
  public void isServiceAreaOptionInInclusionGroupFalse() {
    EsriServiceAreaValidationOption option =
        EsriServiceAreaValidationOption.builder()
            .setLabel("Test")
            .setId("Test")
            .setUrl("/query")
            .setAttribute("CITYNAME")
            .build();

    assertEquals(false, option.isServiceAreaOptionInInclusionGroup(inclusionGroup));
  }
}
