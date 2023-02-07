package services.geo.esri;

import static org.junit.Assert.assertEquals;
import static play.mvc.Results.ok;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;
import services.geo.AddressLocation;
import services.geo.ServiceAreaInclusion;

public class EsriServiceAreaValidationOptionTest {
  private Config config;
  EsriServiceAreaValidationConfig esriServiceAreaValidationConfig;
  private EsriServiceAreaValidationOption esriServiceAreaValidationOption;
  private AddressLocation location;

  // setup for service area validation
  private Server server;
  private WSClient ws;
  private EsriClient client;
  private ImmutableList<ServiceAreaInclusion> inclusionGroup;

  @Before
  // setup Server to return mock data from JSON files
  public void setup() {
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
            .setLongitude(-122.3360380354971)
            .setLatitude(47.578374020558954)
            .setWellKnownId(4326)
            .build();

    server =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET("/query")
                    .routingTo(request -> ok().sendResource("esri/serviceAreaFeatures.json"))
                    .build());
    ws = play.test.WSTestClient.newClient(server.httpPort());

    client = new EsriClient(config, esriServiceAreaValidationConfig, ws);

    inclusionGroup =
        client
            .getServiceAreaInclusionGroup(esriServiceAreaValidationOption, location)
            .toCompletableFuture()
            .join();
  }

  @After
  public void tearDown() throws IOException {
    try {
      ws.close();
    } finally {
      server.stop();
    }
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
