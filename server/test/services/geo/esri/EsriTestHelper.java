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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;
import org.apache.commons.lang3.NotImplementedException;
import play.cache.SyncCacheApi;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;
import services.geo.AddressLocation;
import services.settings.SettingsManifest;

/**
 * This class creates test Play webservers. Each option from the TestType enum corresponds to a
 * specific setup for an endpoint for either address correction or service area validation, and
 * returns specific results from files in the test resources directory.
 */
public class EsriTestHelper {
  // Basically here because java doesn't have built in tuples
  private static class ServerSettings {
    private final Server server;
    private final WSClient wsClient;
    private final EsriClient esriClient;

    ServerSettings(Server server, WSClient wsClient, EsriClient esriClient) {
      this.server = server;
      this.wsClient = wsClient;
      this.esriClient = esriClient;
    }

    Server getServer() {
      return server;
    }

    WSClient getWsClient() {
      return wsClient;
    }

    EsriClient getEsriClient() {
      return esriClient;
    }
  }

  public enum TestType {
    STANDARD,
    STANDARD_WITH_LINE_2,
    NO_CANDIDATES,
    EMPTY_RESPONSE,
    ESRI_ERROR_RESPONSE,
    ERROR,
    SERVICE_AREA_VALIDATION,
    SERVICE_AREA_VALIDATION_ERROR,
    SERVICE_AREA_VALIDATION_NOT_INCLUDED,
    SERVICE_AREA_VALIDATION_NO_FEATURES,
    FAKE,
    MULTIPLE_ENDPOINTS,
    LEGACY_SINGLE_URL_CONFIG_SETTING
  }

  private static final Clock CLOCK = Clock.system(ZoneId.of("America/Los_Angeles"));
  private static final Config CONFIG = ConfigFactory.load();
  private SettingsManifest settingsManifest;

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
  private final ObjectMapper mapper;

  public EsriTestHelper(TestType testType, ObjectMapper mapper, SyncCacheApi syncCacheApi) {
    this.mapper = checkNotNull(mapper);
    this.settingsManifest = new SettingsManifest(CONFIG, syncCacheApi);


    ServerSettings serverSettings =
        switch (testType) {
          case STANDARD ->
              createServerSettingsThatReturnOk(
                  "/findAddressCandidates", "esri/findAddressCandidates.json");
          case STANDARD_WITH_LINE_2 ->
              createServerSettingsThatReturnOk(
                  "/findAddressCandidates", "esri/findAddressCandidatesWithLine2.json");
          case NO_CANDIDATES ->
              createServerSettingsThatReturnOk(
                  "/findAddressCandidates", "esri/findAddressCandidatesNoCandidates.json");
          case EMPTY_RESPONSE ->
              createServerSettingsThatReturnOk(
                  "/findAddressCandidates", "esri/findAddressCandidatesEmptyResponse.json");
          case ESRI_ERROR_RESPONSE ->
              createServerSettingsThatReturnOk(
                  "/findAddressCandidates", "esri/esriErrorResponse.json");
          case ERROR -> createServerSettingsThatReturnError("/findAddressCandidates");
          case SERVICE_AREA_VALIDATION ->
              createServerSettingsThatReturnOk("/query", "esri/serviceAreaFeatures.json");
          case SERVICE_AREA_VALIDATION_NOT_INCLUDED ->
              createServerSettingsThatReturnOk("/query", "esri/serviceAreaFeaturesNotInArea.json");
          case SERVICE_AREA_VALIDATION_NO_FEATURES ->
              createServerSettingsThatReturnOk("/query", "esri/serviceAreaFeaturesNoFeatures.json");
          case SERVICE_AREA_VALIDATION_ERROR -> createServerSettingsThatReturnError("/query");
          case LEGACY_SINGLE_URL_CONFIG_SETTING ->
              createServerSettingsUsingOldConfigValueThatReturnOk();
          case MULTIPLE_ENDPOINTS -> createServerSettingsThatReturnMultiEndpoints();
          case FAKE -> createServerSettingsThatReturnFakeClient();
          default -> {
            throw new NotImplementedException(
                String.format("Could not create EsriTestHelper for TestType: %s", testType));
          }
        };

    server = serverSettings.getServer();
    ws = serverSettings.getWsClient();
    client = serverSettings.getEsriClient();
  }

  private ServerSettings createServerSettingsThatReturnOk(String endpoint, String resource) {
    Server server =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET(endpoint)
                    .routingTo(request -> ok().sendResource(resource))
                    .build());

    WSClient wsClient = play.test.WSTestClient.newClient(server.httpPort());

    RealEsriClient esriClient =
        new RealEsriClient(
          settingsManifest, CLOCK, ESRI_SERVICE_AREA_VALIDATION_CONFIG, wsClient, mapper);

    // overwrite to not include base URL so it uses the mock service
    esriClient.ESRI_FIND_ADDRESS_CANDIDATES_URLS =
        ImmutableList.<String>builder().add("/findAddressCandidates").build();

    return new ServerSettings(server, wsClient, esriClient);
  }

  private ServerSettings createServerSettingsThatReturnError(String endpoint) {
    Server server =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET(endpoint)
                    .routingTo(
                        request -> internalServerError("{ \"Error\": \"An error has occurred\"}"))
                    .build());

    WSClient wsClient = play.test.WSTestClient.newClient(server.httpPort());

    RealEsriClient esriClient =
        new RealEsriClient(
          settingsManifest, CLOCK, ESRI_SERVICE_AREA_VALIDATION_CONFIG, wsClient, mapper);

    // overwrite to not include base URL so it uses the mock service
    esriClient.ESRI_FIND_ADDRESS_CANDIDATES_URLS =
        ImmutableList.<String>builder().add("/findAddressCandidates").build();

    return new ServerSettings(server, wsClient, esriClient);
  }

  private ServerSettings createServerSettingsUsingOldConfigValueThatReturnOk() {
    Server server =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET("/findAddressCandidates")
                    .routingTo(request -> ok().sendResource("esri/findAddressCandidates.json"))
                    .build());

    WSClient wsClient = play.test.WSTestClient.newClient(server.httpPort());

    SettingsManifest mockSettingsManifest = mock();
    when(mockSettingsManifest.getEsriFindAddressCandidatesUrls())
        .thenReturn(Optional.of(ImmutableList.of()));
    when(mockSettingsManifest.getEsriFindAddressCandidatesUrl())
        .thenReturn(Optional.of("/findAddressCandidates"));

    RealEsriClient esriClient =
        new RealEsriClient(
            mockSettingsManifest, CLOCK, ESRI_SERVICE_AREA_VALIDATION_CONFIG, wsClient, mapper);

    return new ServerSettings(server, wsClient, esriClient);
  }

  private ServerSettings createServerSettingsThatReturnMultiEndpoints() {
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
    when(mockSettingsManifest.getEsriFindAddressCandidatesUrls())
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
            mockSettingsManifest, CLOCK, ESRI_SERVICE_AREA_VALIDATION_CONFIG, wsClient, mapper);

    return new ServerSettings(server, wsClient, esriClient);
  }

  private ServerSettings createServerSettingsThatReturnFakeClient() {
    return new ServerSettings(
        null, null, new FakeEsriClient(CLOCK, ESRI_SERVICE_AREA_VALIDATION_CONFIG, mapper));
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
