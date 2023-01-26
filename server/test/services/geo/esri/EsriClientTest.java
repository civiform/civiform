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

public class EsriClientTest {
  private Config config;
  private Server server;
  private WSClient ws;
  private EsriClient client;

  // setup for no canidates in response
  private Server serverNoCandidates;
  private WSClient wsNoCandidates;
  private EsriClient clientNoCandidates;

  // setup for internal server error returned from response
  private Server serverError;
  private WSClient wsError;
  private EsriClient clientError;

  // setup for service area validation
  private Server serverValidation;
  private WSClient wsValidation;
  private EsriClient clientValidation;

  // setup for service area validation with error returned from response
  private Server serverValidationError;
  private WSClient wsValidationError;
  private EsriClient clientValidationError;

  // setup for service area validation with service area not in response features
  private Server serverValidationNotIncluded;
  private WSClient wsValidationNotIncluded;
  private EsriClient clientValidationNotIncluded;

  // setup for service area validation with no features in response
  private Server serverValidationNoFeatures;
  private WSClient wsValidationNoFfeatures;
  private EsriClient clientValidationNoFeatures;

  @Before
  // setup Servers to return mock data from JSON files
  public void setup() {
    config = ConfigFactory.load();
    server =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET("/findAddressCandidates")
                    .routingTo(request -> ok().sendResource("esri/findAddressCandidates.json"))
                    .build());
    ws = play.test.WSTestClient.newClient(server.httpPort());
    client = new EsriClient(config, ws);
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

    clientNoCandidates = new EsriClient(config, wsNoCandidates);
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

    clientError = new EsriClient(config, wsError);
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

    clientValidation = new EsriClient(config, wsValidation);

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

    clientValidationError = new EsriClient(config, wsValidationError);

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

    clientValidationNotIncluded = new EsriClient(config, wsValidationNotIncluded);

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

    clientValidationNoFeatures = new EsriClient(config, wsValidationNoFeatures);
  }

  @After
  public void tearDown() throws IOException {
    try {
      ws.close();
      wsNoCandidates.close();
    } finally {
      server.stop();
      serverNoCandidates.stop();
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
    assertEquals(1, candidates.size());
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
            .setState("California")
            .setZip("92373")
            .build();

    CompletionStage<Optional<AddressSuggestionGroup>> group = client.getAddressSuggestions(address);
    ImmutableList<AddressSuggestion> suggestions =
        group.toCompletableFuture().join().get().getAddressSuggestions();
    // first item is guaranteed to be here since the response is taken from  JSON file
    Optional<AddressSuggestion> addressSuggestion = suggestions.stream().findFirst();
    assertThat(addressSuggestion.isPresent()).isTrue();
    String street = addressSuggestion.get().getAddress().getStreet();
    assertEquals("\"380 New York St\"", street);
  }

  @Test
  public void getAddressSuggestionsWithNoCandidates() {
    Address address =
        Address.builder()
            .setStreet("380 New York St")
            .setLine2("")
            .setCity("Redlands")
            .setState("California")
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
            .setState("California")
            .setZip("92373")
            .build();

    CompletionStage<Optional<AddressSuggestionGroup>> group =
        clientError.getAddressSuggestions(address);
    assertEquals(Optional.empty(), group.toCompletableFuture().join());
  }

  @Test
  public void fetchServiceAreaFeatures() {
    AddressLocation location =
        AddressLocation.builder()
            .setLongitude(-122.3360380354971)
            .setLatitude(47.578374020558954)
            .setWellKnownId(4326)
            .build();

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
    AddressLocation location =
        AddressLocation.builder()
            .setLongitude(-122.3360380354971)
            .setLatitude(47.578374020558954)
            .setWellKnownId(4326)
            .build();

    Optional<JsonNode> maybeResp =
        clientValidationError
            .fetchServiceAreaFeatures(location, "/query")
            .toCompletableFuture()
            .join();
    assertEquals(Optional.empty(), maybeResp);
  }

  @Test
  public void isAddressLocationInServiceAreaShouldBeTrue() {
    AddressLocation location =
        AddressLocation.builder()
            .setLongitude(-122.3360380354971)
            .setLatitude(47.578374020558954)
            .setWellKnownId(4326)
            .build();

    Optional<Boolean> isInServiceArea =
        clientValidation
            .isAddressLocationInServiceArea("Seattle", location)
            .toCompletableFuture()
            .join();
    assertEquals(true, isInServiceArea.get());
  }

  @Test
  public void isAddressLocationInServiceAreaNotIncluded() {
    AddressLocation location =
        AddressLocation.builder()
            .setLongitude(-122.3360380354971)
            .setLatitude(47.578374020558954)
            .setWellKnownId(4326)
            .build();

    Optional<Boolean> isInServiceArea =
        clientValidationNotIncluded
            .isAddressLocationInServiceArea("Seattle", location)
            .toCompletableFuture()
            .join();
    assertEquals(false, isInServiceArea.get());
  }

  @Test
  public void isAddressLocationInServiceAreaNoFeatures() {
    AddressLocation location =
        AddressLocation.builder()
            .setLongitude(-122.3360380354971)
            .setLatitude(47.578374020558954)
            .setWellKnownId(4326)
            .build();

    Optional<Boolean> isInServiceArea =
        clientValidationNoFeatures
            .isAddressLocationInServiceArea("Seattle", location)
            .toCompletableFuture()
            .join();
    assertEquals(false, isInServiceArea.get());
  }
}
