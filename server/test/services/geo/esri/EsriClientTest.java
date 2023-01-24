package services.geo.esri;

import static org.junit.Assert.assertEquals;
import static play.mvc.Results.ok;
import static play.mvc.Results.internalServerError;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
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
import services.geo.AddressSuggestion;
import services.geo.AddressSuggestionGroup;

public class EsriClientTest {
  private Config config;
  private EsriClient client;
  private Server server;
  private WSClient ws;

  // setup for no canidates in response
  private EsriClient clientNoCandidates;
  private Server serverNoCandidates;
  private WSClient wsNoCandidates;

  // setup for internal server error returned from response
  private EsriClient clientError;
  private Server serverError;
  private WSClient wsError;

  @Before
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
    serverNoCandidates = Server.forRouter(
      (components) ->
          RoutingDsl.fromComponents(components)
              .GET("/findAddressCandidates")
              .routingTo(request -> ok().sendResource("esri/findAddressCandidatesNoCandidates.json"))
              .build());
    wsNoCandidates = play.test.WSTestClient.newClient(serverNoCandidates.httpPort());
    
    clientNoCandidates = new EsriClient(config, wsNoCandidates);
    // overwrite to not include base URL so it uses the mock service
    clientNoCandidates.ESRI_FIND_ADDRESS_CANDIDATES_URL = Optional.of("/findAddressCandidates");

    // create a server that returns an internal server error
    serverError = Server.forRouter(
      (components) ->
          RoutingDsl.fromComponents(components)
              .GET("/findAddressCandidates")
              .routingTo(request -> internalServerError("{ \"Error\": \"An error has occurred\"}"))
              .build());
    wsError = play.test.WSTestClient.newClient(serverError.httpPort());
    
    clientError = new EsriClient(config, wsError);
    // overwrite to not include base URL so it uses the mock service
    clientError.ESRI_FIND_ADDRESS_CANDIDATES_URL = Optional.of("/findAddressCandidates");
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
    Optional<JsonNode> maybeResp = client.fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    JsonNode resp = maybeResp.get();
    ArrayNode candidates = (ArrayNode)resp.get("candidates");
    assertEquals(4326, resp.get("spatialReference").get("wkid").asInt());
    assertEquals(1, candidates.size());
  }

  @Test
  public void fetchAddressSuggestionsWithNoCandidates() throws Exception {
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    Optional<JsonNode> maybeResp = clientNoCandidates.fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    JsonNode resp = maybeResp.get();
    ArrayNode candidates = (ArrayNode)resp.get("candidates");
    assertEquals(0, candidates.size());
  }

  @Test
  public void fetchAddressSuggestionsWithError() throws Exception {
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    Optional<JsonNode> maybeResp = clientError.fetchAddressSuggestions(addressJson).toCompletableFuture().get();
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
    String street = suggestions.stream().findFirst().get().getAddress().getStreet();
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

    CompletionStage<Optional<AddressSuggestionGroup>> group = clientNoCandidates.getAddressSuggestions(address);
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

    CompletionStage<Optional<AddressSuggestionGroup>> group = clientError.getAddressSuggestions(address);
    assertEquals(Optional.empty(), group.toCompletableFuture().join());
  }
}
