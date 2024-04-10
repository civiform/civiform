/* This file exists here rather than 'support' so that
 * we are in services.geo.esri and thus have access to
 * ESRI_FIND_ADDRESS_CANDIDATES_URL
 */
package services.geo.esri;

import static com.google.common.base.Preconditions.checkNotNull;
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
import org.apache.commons.lang3.NotImplementedException;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;
import services.geo.AddressLocation;
import services.settings.SettingsManifest;

public class EsriTestHelper {
  // Basically here because java doesn't have built in tuples
  private static class ServerSettings {
    private final Server server;
    private final WSClient wsClient;
    private final EsriClient esriClient;

    public ServerSettings(Server server, WSClient wsClient, EsriClient esriClient) {
      this.server = checkNotNull(server);
      this.wsClient = checkNotNull(wsClient);
      this.esriClient = checkNotNull(esriClient);
    }

    public Server getServer() {
      return server;
    }

    public WSClient getWsClient() {
      return wsClient;
    }

    public EsriClient getEsriClient() {
      return esriClient;
    }
  }

  public enum TestType {
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

  public EsriTestHelper(TestType testType) {
    ServerSettings serverSettings;

    switch (testType) {
      case STANDARD:
        serverSettings =
            createServerSettingsThatReturnOk(
                "/findAddressCandidates", "esri/findAddressCandidates.json");
        break;
      case STANDARD_WITH_LINE_2:
        serverSettings =
            createServerSettingsThatReturnOk(
                "/findAddressCandidates", "esri/findAddressCandidatesWithLine2.json");
        break;
      case NO_CANDIDATES:
        serverSettings =
            createServerSettingsThatReturnOk(
                "/findAddressCandidates", "esri/findAddressCandidatesNoCandidates.json");
        break;
      case ERROR:
        serverSettings = createServerSettingsThatReturnError("/findAddressCandidates");
        break;
      case SERVICE_AREA_VALIDATION:
        serverSettings =
            createServerSettingsThatReturnOk("/query", "esri/serviceAreaFeatures.json");
        break;
      case SERVICE_AREA_VALIDATION_NOT_INCLUDED:
        serverSettings =
            createServerSettingsThatReturnOk("/query", "esri/serviceAreaFeaturesNotInArea.json");
        break;
      case SERVICE_AREA_VALIDATION_NO_FEATURES:
        serverSettings =
            createServerSettingsThatReturnOk("/query", "esri/serviceAreaFeaturesNoFeatures.json");
        break;
      case SERVICE_AREA_VALIDATION_ERROR:
        serverSettings = createServerSettingsThatReturnError("/query");
        break;
      case MULTIPLE_ENDPOINTS:
        serverSettings = createServerSettingsThatReturnMultiEndpoints();
        break;
      case FAKE:
        serverSettings = createServerSettingsThatReturnFakeClient();
        break;
      default:
        throw new NotImplementedException(
            String.format("Could not create EsriTestHelper for TestType: %s", testType));
    }

    server = serverSettings.getServer();
    ws = serverSettings.getWsClient();
    client = serverSettings.getEsriClient();
  }

  private static ServerSettings createServerSettingsThatReturnOk(String endpoint, String resource) {
    Server server =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET(endpoint)
                    .routingTo(request -> ok().sendResource(resource))
                    .build());

    WSClient wsClient = play.test.WSTestClient.newClient(server.httpPort());

    EsriClient esriClient =
        new RealEsriClient(SETTINGS_MANIFEST, CLOCK, ESRI_SERVICE_AREA_VALIDATION_CONFIG, wsClient);

    return new ServerSettings(server, wsClient, esriClient);
  }

  private static ServerSettings createServerSettingsThatReturnError(String endpoint) {
    Server server =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET(endpoint)
                    .routingTo(
                        request -> internalServerError("{ \"Error\": \"An error has occurred\"}"))
                    .build());

    WSClient wsClient = play.test.WSTestClient.newClient(server.httpPort());

    EsriClient esriClient =
        new RealEsriClient(SETTINGS_MANIFEST, CLOCK, ESRI_SERVICE_AREA_VALIDATION_CONFIG, wsClient);

    return new ServerSettings(server, wsClient, esriClient);
  }

  private static ServerSettings createServerSettingsThatReturnMultiEndpoints() {
    Server server =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET("/findAddressCandidates1")
                    .routingTo(
                        request ->
                            ok().sendResource("esri/findAddressCandidatesNo100PercentMatch.json"))
                    .GET("/findAddressCandidates2")
                    .routingTo(
                        request -> ok().sendResource("esri/findAddressCandidatesNoCandidates.json"))
                    .GET("/findAddressCandidates3")
                    .routingTo(
                        request -> ok().sendResource("esri/findAddressCandidatesWithLine2.json"))
                    .GET("/findAddressCandidates4")
                    .routingTo(request -> ok().sendResource("esri/findAddressCandidates.json"))
                    .build());

    WSClient wsClient = play.test.WSTestClient.newClient(server.httpPort());

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

    RealEsriClient esriClient =
        new RealEsriClient(
            mockSettingsManifest, CLOCK, ESRI_SERVICE_AREA_VALIDATION_CONFIG, wsClient);

    return new ServerSettings(server, wsClient, esriClient);
  }

  private static ServerSettings createServerSettingsThatReturnFakeClient() {
    return new ServerSettings(
        null, null, new FakeEsriClient(CLOCK, ESRI_SERVICE_AREA_VALIDATION_CONFIG));
  }

  public EsriClient getClient() {
    return client;
  }

  public void stopServer() throws IOException {
    try {
      if (ws != null) {
        ws.close();
      }
    } finally {
      if (server != null) {
        server.stop();
      }
    }
  }
}
