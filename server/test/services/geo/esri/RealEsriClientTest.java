package services.geo.esri;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static play.mvc.Results.internalServerError;
import static play.mvc.Results.ok;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;
import services.Address;
import services.geo.AddressLocation;
import services.geo.AddressSuggestion;
import services.geo.AddressSuggestionGroup;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;

public class RealEsriClientTest {
  private Config config;
  EsriServiceAreaValidationConfig esriServiceAreaValidationConfig;
  private EsriServiceAreaValidationOption esriServiceAreaValidationOption;
  private AddressLocation location;
  private Server server;
  private WSClient ws;
  private RealEsriClient client;

  // setup for no canidates in response
  private Server serverNoCandidates;
  private WSClient wsNoCandidates;
  private RealEsriClient clientNoCandidates;

  // setup for internal server error returned from response
  private Server serverError;
  private WSClient wsError;
  private RealEsriClient clientError;

  // setup for service area validation
  private Server serverValidation;
  private WSClient wsValidation;
  private RealEsriClient clientValidation;

  // setup for service area validation with error returned from response
  private Server serverValidationError;
  private WSClient wsValidationError;
  private RealEsriClient clientValidationError;

  // setup for service area validation with service area not in response features
  private Server serverValidationNotIncluded;
  private WSClient wsValidationNotIncluded;
  private RealEsriClient clientValidationNotIncluded;

  // setup for service area validation with no features in response
  private Server serverValidationNoFeatures;
  private WSClient wsValidationNoFeatures;
  private RealEsriClient clientValidationNoFeatures;

  @Before
  // setup Servers to return mock data from JSON files
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
            .setLongitude(-122.3360380354971)
            .setLatitude(47.578374020558954)
            .setWellKnownId(4326)
            .build();
    server =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET("/findAddressCandidates")
                    .routingTo(request -> ok().sendResource("esri/findAddressCandidates.json"))
                    .build());
    ws = play.test.WSTestClient.newClient(server.httpPort());
    client = new RealEsriClient(config, clock, esriServiceAreaValidationConfig, ws);
    // overwrite to not include base URL so it uses the mock service
    client.ESRI_FIND_ADDRESS_CANDIDATES_URL = Optional.of("/findAddressCandidates");

    // create a server that returns no candidates
    serverNoCandidates =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET("/findAddressCandidates")
                    .routingTo(
                        request -> ok().sendResource("esri/findAddressCandidatesNoCandidates.json"))
                    .build());
    wsNoCandidates = play.test.WSTestClient.newClient(serverNoCandidates.httpPort());

    clientNoCandidates =
        new RealEsriClient(config, clock, esriServiceAreaValidationConfig, wsNoCandidates);
    // overwrite to not include base URL so it uses the mock service
    clientNoCandidates.ESRI_FIND_ADDRESS_CANDIDATES_URL = Optional.of("/findAddressCandidates");

    // create a server that returns an internal server error
    serverError =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET("/findAddressCandidates")
                    .routingTo(
                        request -> internalServerError("{ \"Error\": \"An error has occurred\"}"))
                    .build());
    wsError = play.test.WSTestClient.newClient(serverError.httpPort());

    clientError = new RealEsriClient(config, clock, esriServiceAreaValidationConfig, wsError);
    // overwrite to not include base URL so it uses the mock service
    clientError.ESRI_FIND_ADDRESS_CANDIDATES_URL = Optional.of("/findAddressCandidates");

    // create a server for service area validation
    serverValidation =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET("/query")
                    .routingTo(request -> ok().sendResource("esri/serviceAreaFeatures.json"))
                    .build());
    wsValidation = play.test.WSTestClient.newClient(serverValidation.httpPort());

    clientValidation =
        new RealEsriClient(config, clock, esriServiceAreaValidationConfig, wsValidation);

    // create a server for service area validation with error returned
    serverValidationError =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET("/query")
                    .routingTo(
                        request -> internalServerError("{ \"Error\": \"An error has occurred\"}"))
                    .build());
    wsValidationError = play.test.WSTestClient.newClient(serverValidationError.httpPort());

    clientValidationError =
        new RealEsriClient(config, clock, esriServiceAreaValidationConfig, wsValidationError);

    // create a server for service area validation with service area not in response
    serverValidationNotIncluded =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET("/query")
                    .routingTo(
                        request -> ok().sendResource("esri/serviceAreaFeaturesNotInArea.json"))
                    .build());
    wsValidationNotIncluded =
        play.test.WSTestClient.newClient(serverValidationNotIncluded.httpPort());

    clientValidationNotIncluded =
        new RealEsriClient(config, clock, esriServiceAreaValidationConfig, wsValidationNotIncluded);

    // create a server for service area validation with no features in response
    serverValidationNoFeatures =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET("/query")
                    .routingTo(
                        request -> ok().sendResource("esri/serviceAreaFeaturesNoFeatures.json"))
                    .build());
    wsValidationNoFeatures =
        play.test.WSTestClient.newClient(serverValidationNoFeatures.httpPort());

    clientValidationNoFeatures =
        new RealEsriClient(config, clock, esriServiceAreaValidationConfig, wsValidationNoFeatures);
  }

  @After
  public void tearDown() throws IOException {
    try {
      ws.close();
      wsNoCandidates.close();
      wsError.close();
      wsValidation.close();
      wsValidationError.close();
      wsValidationNotIncluded.close();
      wsValidationNoFeatures.close();
    } finally {
      server.stop();
      serverNoCandidates.stop();
      serverError.stop();
      serverValidation.stop();
      serverValidationError.stop();
      serverValidationNotIncluded.stop();
      serverValidationNoFeatures.stop();
    }
  }

  @Test
  public void fetchAddressSuggestions() throws Exception {
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    Optional<JsonNode> maybeResp =
        client.fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    JsonNode resp = maybeResp.get();
    ArrayNode candidates = (ArrayNode) resp.get("candidates");
    assertEquals(4326, resp.get("spatialReference").get("wkid").asInt());
    assertEquals(4, candidates.size());
  }

  @Test
  public void fetchAddressSuggestionsWithNoCandidates() throws Exception {
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    Optional<JsonNode> maybeResp =
        clientNoCandidates.fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    JsonNode resp = maybeResp.get();
    ArrayNode candidates = (ArrayNode) resp.get("candidates");
    assertEquals(0, candidates.size());
  }

  @Test
  public void fetchAddressSuggestionsWithError() throws Exception {
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    Optional<JsonNode> maybeResp =
        clientError.fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    assertEquals(Optional.empty(), maybeResp);
  }

  @Test
  public void getAddressSuggestions() {
    Address address =
        Address.builder()
            .setStreet("380 New York St")
            .setLine2("")
            .setCity("Redlands")
            .setState("CA")
            .setZip("92373")
            .build();

    CompletionStage<Optional<AddressSuggestionGroup>> group = client.getAddressSuggestions(address);
    ImmutableList<AddressSuggestion> suggestions =
        group.toCompletableFuture().join().get().getAddressSuggestions();
    // first item is guaranteed to be here since the response is taken from  JSON file
    Optional<AddressSuggestion> addressSuggestion = suggestions.stream().findFirst();
    assertThat(addressSuggestion.isPresent()).isTrue();
    String street = addressSuggestion.get().getAddress().getStreet();
    assertEquals("Address In Area", street);
  }

  @Test
  public void getAddressSuggestionsIncludesOriginalAddress() {
    Address address =
        Address.builder()
            .setStreet("380 New York St")
            .setLine2("")
            .setCity("Redlands")
            .setState("CA")
            .setZip("92373")
            .build();

    CompletionStage<Optional<AddressSuggestionGroup>> group = client.getAddressSuggestions(address);
    Address originalAddress = group.toCompletableFuture().join().get().getOriginalAddress();

    assertEquals(address.getStreet(), originalAddress.getStreet());
    assertEquals(address.getLine2(), originalAddress.getLine2());
    assertEquals(address.getCity(), originalAddress.getCity());
    assertEquals(address.getState(), originalAddress.getState());
    assertEquals(address.getZip(), originalAddress.getZip());
  }

  @Test
  public void getAddressSuggestionsWithNoCandidates() {
    Address address =
        Address.builder()
            .setStreet("380 New York St")
            .setLine2("")
            .setCity("Redlands")
            .setState("CA")
            .setZip("92373")
            .build();

    CompletionStage<Optional<AddressSuggestionGroup>> group =
        clientNoCandidates.getAddressSuggestions(address);
    ImmutableList<AddressSuggestion> suggestions =
        group.toCompletableFuture().join().get().getAddressSuggestions();
    assertEquals(0, suggestions.size());
  }

  @Test
  public void getAddressSuggestionsWithError() {
    Address address =
        Address.builder()
            .setStreet("380 New York St")
            .setLine2("")
            .setCity("Redlands")
            .setState("CA")
            .setZip("92373")
            .build();

    CompletionStage<Optional<AddressSuggestionGroup>> group =
        clientError.getAddressSuggestions(address);
    assertEquals(Optional.empty(), group.toCompletableFuture().join());
  }

  @Test
  public void fetchServiceAreaFeatures() {
    Optional<JsonNode> maybeResp =
        clientValidation.fetchServiceAreaFeatures(location, "/query").toCompletableFuture().join();
    JsonNode resp = maybeResp.get();
    ReadContext ctx = JsonPath.parse(resp.toString());
    List<String> features = ctx.read("features[*].attributes.CITYNAME");
    Optional<String> feature = features.stream().filter(val -> "Seattle".equals(val)).findFirst();
    assertThat(feature.isPresent()).isTrue();
    assertEquals("Seattle", feature.get());
  }

  @Test
  public void fetchServiceAreaFeaturesWithError() {
    Optional<JsonNode> maybeResp =
        clientValidationError
            .fetchServiceAreaFeatures(location, "/query")
            .toCompletableFuture()
            .join();
    assertEquals(Optional.empty(), maybeResp);
  }

  @Test
  public void getServiceAreaInclusionGroup() {
    ImmutableList<ServiceAreaInclusion> inclusionList =
        clientValidation
            .getServiceAreaInclusionGroup(esriServiceAreaValidationOption, location)
            .toCompletableFuture()
            .join();
    Optional<ServiceAreaInclusion> area = inclusionList.stream().findFirst();
    assertThat(area.isPresent()).isTrue();
    assertEquals("Seattle", area.get().getServiceAreaId());
    assertEquals(ServiceAreaState.IN_AREA, area.get().getState());
    assertThat(area.get().getTimeStamp()).isInstanceOf(Long.class);
  }

  @Test
  public void getServiceAreaInclusionGroupAreaNotIncluded() {
    ImmutableList<ServiceAreaInclusion> inclusionList =
        clientValidationNotIncluded
            .getServiceAreaInclusionGroup(esriServiceAreaValidationOption, location)
            .toCompletableFuture()
            .join();
    Optional<ServiceAreaInclusion> area = inclusionList.stream().findFirst();
    assertThat(area.isPresent()).isTrue();
    assertEquals("Seattle", area.get().getServiceAreaId());
    assertEquals(ServiceAreaState.NOT_IN_AREA, area.get().getState());
    assertThat(area.get().getTimeStamp()).isInstanceOf(Long.class);
  }

  @Test
  public void getServiceAreaInclusionGroupNoFeatures() {
    ImmutableList<ServiceAreaInclusion> inclusionList =
        clientValidationNoFeatures
            .getServiceAreaInclusionGroup(esriServiceAreaValidationOption, location)
            .toCompletableFuture()
            .join();
    Optional<ServiceAreaInclusion> area = inclusionList.stream().findFirst();
    assertThat(area.isPresent()).isTrue();
    assertEquals("Seattle", area.get().getServiceAreaId());
    assertEquals(ServiceAreaState.NOT_IN_AREA, area.get().getState());
    assertThat(area.get().getTimeStamp()).isInstanceOf(Long.class);
  }

  @Test
  public void getServiceAreaInclusionGroupError() {
    ImmutableList<ServiceAreaInclusion> inclusionList =
        clientValidationError
            .getServiceAreaInclusionGroup(esriServiceAreaValidationOption, location)
            .toCompletableFuture()
            .join();
    Optional<ServiceAreaInclusion> area = inclusionList.stream().findFirst();
    assertThat(area.isPresent()).isTrue();
    assertEquals("Seattle", area.get().getServiceAreaId());
    assertEquals(ServiceAreaState.FAILED, area.get().getState());
    assertThat(area.get().getTimeStamp()).isInstanceOf(Long.class);
  }
}
