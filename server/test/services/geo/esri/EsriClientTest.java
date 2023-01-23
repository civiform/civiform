package services.geo.esri;

import static org.junit.Assert.assertEquals;
import static play.mvc.Results.ok;

import com.fasterxml.jackson.databind.JsonNode;
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
  private EsriClient client;
  private WSClient ws;
  private Server server;
  private Config config;

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
  public void fetchAddressSuggestions() throws Exception {
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    Optional<JsonNode> maybeResp = client.fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    JsonNode resp = maybeResp.get();
    assertEquals(4326, resp.get("spatialReference").get("wkid").asInt());
  }

  @Test
  public void getAddressSuggestionGroup() {
    Address address =
        Address.builder()
            .setStreet("380 New York St")
            .setLine2("")
            .setCity("Redlands")
            .setState("California")
            .setZip("92373")
            .build();

    CompletionStage<Optional<AddressSuggestionGroup>> group = client.getAddressSuggestionGroup(address);
    ImmutableList<AddressSuggestion> suggestions =
        group.toCompletableFuture().join().get().getAddressSuggestions();
    String street = suggestions.stream().findFirst().get().getAddress().getStreet();
    assertEquals("\"380 New York St\"", street);
  }
}
