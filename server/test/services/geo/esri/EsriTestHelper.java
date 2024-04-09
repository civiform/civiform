/* This file exists here rather than 'support' so that
 * we are in services.geo.esri and thus have access to
 * ESRI_FIND_ADDRESS_CANDIDATES_URL
 */
package services.geo.esri;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Results.internalServerError;
import static play.mvc.Results.ok;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;
import services.geo.AddressLocation;
import services.settings.SettingsManifest;

public class EsriTestHelper {
  public static enum TestType {
    STANDARD,
    STANDARD_WITH_LINE_2,
    NO_CANDIDATES,
    ERROR,
    SERVICE_AREA_VALIDATION,
    SERVICE_AREA_VALIDATION_ERROR,
    SERVICE_AREA_VALIDATION_NOT_INCLUDED,
    SERVICE_AREA_VALIDATION_NO_FEATURES,
    FAKE,
    MULTIPLE_ENDPOINTS
  }

  private static final Clock CLOCK = Clock.system(ZoneId.of("America/Los_Angeles"));
  private static final Config CONFIG = ConfigFactory.load();
  private static final SettingsManifest SETTINGS_MANIFEST = new SettingsManifest(CONFIG);

  private static final EsriServiceAreaValidationConfig ESRI_SERVICE_AREA_VALIDATION_CONFIG =
      new EsriServiceAreaValidationConfig(CONFIG);

  public static final AddressLocation LOCATION =
      AddressLocation.builder()
          .setLongitude(-122.3360380354971)
          .setLatitude(47.578374020558954)
          .setWellKnownId(4326)
          .build();
  public static final EsriServiceAreaValidationOption ESRI_SERVICE_AREA_VALIDATION_OPTION =
      EsriServiceAreaValidationOption.builder()
          .setLabel("Seattle")
          .setId("Seattle")
          .setUrl("/query")
          .setAttribute("CITYNAME")
          .build();

  private final Server server;
  private final WSClient ws;
  private final EsriClient client;

  public EsriTestHelper(TestType testType) throws Exception {
    switch (testType) {
      case STANDARD:
        server =
            Server.forRouter(
                (components) ->
                    RoutingDsl.fromComponents(components)
                        .GET("/findAddressCandidates")
                        .routingTo(request -> ok().sendResource("esri/findAddressCandidates.json"))
                        .build());
        break;
      case STANDARD_WITH_LINE_2:
        server =
            Server.forRouter(
                (components) ->
                    RoutingDsl.fromComponents(components)
                        .GET("/findAddressCandidates")
                        .routingTo(
                            request ->
                                ok().sendResource("esri/findAddressCandidatesWithLine2.json"))
                        .build());
        break;
      case NO_CANDIDATES:
        server =
            Server.forRouter(
                (components) ->
                    RoutingDsl.fromComponents(components)
                        .GET("/findAddressCandidates")
                        .routingTo(
                            request ->
                                ok().sendResource("esri/findAddressCandidatesNoCandidates.json"))
                        .build());
        break;
      case ERROR:
        server =
            Server.forRouter(
                (components) ->
                    RoutingDsl.fromComponents(components)
                        .GET("/findAddressCandidates")
                        .routingTo(
                            request ->
                                internalServerError("{ \"Error\": \"An error has occurred\"}"))
                        .build());
        break;
      case SERVICE_AREA_VALIDATION:
        server =
            Server.forRouter(
                (components) ->
                    RoutingDsl.fromComponents(components)
                        .GET("/query")
                        .routingTo(request -> ok().sendResource("esri/serviceAreaFeatures.json"))
                        .build());
        break;
      case SERVICE_AREA_VALIDATION_ERROR:
        server =
            Server.forRouter(
                (components) ->
                    RoutingDsl.fromComponents(components)
                        .GET("/query")
                        .routingTo(
                            request ->
                                internalServerError("{ \"Error\": \"An error has occurred\"}"))
                        .build());
        break;
      case SERVICE_AREA_VALIDATION_NOT_INCLUDED:
        server =
            Server.forRouter(
                (components) ->
                    RoutingDsl.fromComponents(components)
                        .GET("/query")
                        .routingTo(
                            request -> ok().sendResource("esri/serviceAreaFeaturesNotInArea.json"))
                        .build());
        break;
      case SERVICE_AREA_VALIDATION_NO_FEATURES:
        server =
            Server.forRouter(
                (components) ->
                    RoutingDsl.fromComponents(components)
                        .GET("/query")
                        .routingTo(
                            request -> ok().sendResource("esri/serviceAreaFeaturesNoFeatures.json"))
                        .build());
        break;
      case MULTIPLE_ENDPOINTS:
        server =
            Server.forRouter(
                (components) ->
                    RoutingDsl.fromComponents(components)
                        .GET("/findAddressCandidates1")
                        .routingTo(
                            request ->
                                ok().sendResource(
                                        "esri/findAddressCandidatesNo100PercentMatch.json"))
                        .GET("/findAddressCandidates2")
                        .routingTo(
                            request ->
                                ok().sendResource("esri/findAddressCandidatesNoCandidates.json"))
                        .GET("/findAddressCandidates3")
                        .routingTo(
                            request ->
                                ok().sendResource("esri/findAddressCandidatesWithLine2.json"))
                        .GET("/findAddressCandidates4")
                        .routingTo(request -> ok().sendResource("esri/findAddressCandidates.json"))
                        .build());

        ws = play.test.WSTestClient.newClient(server.httpPort());

        SettingsManifest mockSettingsManifest = mock();
        when(mockSettingsManifest.getEsriFindAddressCandidatesUrl())
            .thenReturn(
                Optional.of(
                    ImmutableList.<String>builder()
                        .add("/findAddressCandidates1")
                        .add("/findAddressCandidates2")
                        .add("/findAddressCandidates3")
                        .add("/findAddressCandidates4")
                        .build()));

        RealEsriClient realClient =
            new RealEsriClient(
                mockSettingsManifest, CLOCK, ESRI_SERVICE_AREA_VALIDATION_CONFIG, ws);

        client = realClient;
        return;
      case FAKE:
        server = null;
        ws = null;
        client = new FakeEsriClient(CLOCK, ESRI_SERVICE_AREA_VALIDATION_CONFIG);
        return;
      default:
        // Not really possible
        throw new Exception("Unknown server type");
    }
    ws = play.test.WSTestClient.newClient(server.httpPort());

    RealEsriClient realClient =
        new RealEsriClient(SETTINGS_MANIFEST, CLOCK, ESRI_SERVICE_AREA_VALIDATION_CONFIG, ws);
    // overwrite to not include base URL so it uses the mock service
    realClient.ESRI_FIND_ADDRESS_CANDIDATES_URLS =
        ImmutableList.<String>builder().add("/findAddressCandidates").build();
    client = realClient;
  }

  public EsriClient getClient() {
    return client;
  }

  public void stopServer() throws IOException {
    try {
      ws.close();
    } finally {
      server.stop();
    }
  }
}
