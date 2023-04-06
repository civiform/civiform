package services.geo.esri;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import play.libs.Json;
import services.geo.AddressLocation;
import services.geo.esri.EsriTestHelper.TestType;

public class FakeEsriClientTest {
  private final EsriTestHelper helper;

  public FakeEsriClientTest() throws Exception {
    helper = new EsriTestHelper(TestType.FAKE);
  }

  @Test
  public void fetchAddressSuggestions() throws Exception {
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "Legit Address");
    Optional<JsonNode> maybeResp =
        helper.getClient().fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    assertThat(maybeResp.isPresent()).isTrue();
    JsonNode resp = maybeResp.get();
    ArrayNode candidates = (ArrayNode) resp.get("candidates");
    assertEquals(4326, resp.get("spatialReference").get("wkid").asInt());
    assertEquals(5, candidates.size());
  }

  @Test
  public void fetchAddressSuggestionsWithNoCandidates() throws Exception {
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "Bogus Address");
    Optional<JsonNode> maybeResp =
        helper.getClient().fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    assertThat(maybeResp.isPresent()).isTrue();
    JsonNode resp = maybeResp.get();
    ArrayNode candidates = (ArrayNode) resp.get("candidates");
    assertEquals(0, candidates.size());
  }

  @Test
  public void fetchAddressSuggestionsWithError() throws Exception {
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "Error Address");
    Optional<JsonNode> maybeResp =
        helper.getClient().fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    assertEquals(Optional.empty(), maybeResp);
  }

  @Test
  public void fetchAddressSuggestionsInvalidAddress() throws Exception {
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "oops");
    assertThatThrownBy(() -> helper.getClient().fetchAddressSuggestions(addressJson))
        .isInstanceOf(InvalidFakeAddressException.class);
  }

  @Test
  public void fetchServiceAreaFeatures() {
    AddressLocation location =
        AddressLocation.builder().setLongitude(-100).setLatitude(100).setWellKnownId(4326).build();
    Optional<JsonNode> maybeResp =
        helper
            .getClient()
            .fetchServiceAreaFeatures(location, "/query")
            .toCompletableFuture()
            .join();
    assertThat(maybeResp.isPresent()).isTrue();
    JsonNode resp = maybeResp.get();
    ReadContext ctx = JsonPath.parse(resp.toString());
    List<String> features = ctx.read("features[*].attributes.CITYNAME");
    Optional<String> feature = features.stream().filter(val -> "Seattle".equals(val)).findFirst();
    assertThat(feature.isPresent()).isTrue();
    assertEquals("Seattle", feature.get());
  }

  @Test
  public void fetchServiceAreaFeaturesNoFeatures() {
    AddressLocation location =
        AddressLocation.builder().setLongitude(-101).setLatitude(101).setWellKnownId(4326).build();
    Optional<JsonNode> maybeResp =
        helper
            .getClient()
            .fetchServiceAreaFeatures(location, "/query")
            .toCompletableFuture()
            .join();
    assertThat(maybeResp.isPresent()).isTrue();
    JsonNode resp = maybeResp.get();
    ReadContext ctx = JsonPath.parse(resp.toString());
    List<String> features = ctx.read("features[*]");
    assertEquals(0, features.size());
  }

  @Test
  public void fetchServiceAreaFeaturesNotInArea() {
    AddressLocation location =
        AddressLocation.builder().setLongitude(-102).setLatitude(102).setWellKnownId(4326).build();
    Optional<JsonNode> maybeResp =
        helper
            .getClient()
            .fetchServiceAreaFeatures(location, "/query")
            .toCompletableFuture()
            .join();
    assertThat(maybeResp.isPresent()).isTrue();
    JsonNode resp = maybeResp.get();
    ReadContext ctx = JsonPath.parse(resp.toString());
    List<String> features = ctx.read("features[*].attributes.CITYNAME");
    Optional<String> feature = features.stream().filter(val -> "Seattle".equals(val)).findFirst();
    assertThat(feature.isPresent()).isFalse();
  }

  @Test
  public void fetchServiceAreaFeaturesWithError() {
    AddressLocation location =
        AddressLocation.builder().setLongitude(-103).setLatitude(103).setWellKnownId(4326).build();
    Optional<JsonNode> maybeResp =
        helper
            .getClient()
            .fetchServiceAreaFeatures(location, "/query")
            .toCompletableFuture()
            .join();
    assertEquals(Optional.empty(), maybeResp);
  }
}
