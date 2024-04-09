package services.geo.esri;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Test;
import play.libs.Json;
import services.geo.esri.EsriTestHelper.TestType;

public class RealEsriClientTest {
  private EsriTestHelper helper;

  @After
  public void tearDown() throws IOException {
    helper.stopServer();
  }

  @Test
  public void fetchAddressSuggestions() throws Exception {
    helper = new EsriTestHelper(TestType.STANDARD);
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    Optional<JsonNode> maybeResp =
        helper.getClient().fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    JsonNode resp = maybeResp.get();
    ArrayNode candidates = (ArrayNode) resp.get("candidates");
    assertThat(resp.get("spatialReference").get("wkid").asInt()).isEqualTo(4326);
    assertThat(candidates).hasSize(5);
  }

  @Test
  public void fetchAddressSuggestionsHavingLine2Populated() throws Exception {
    helper = new EsriTestHelper(TestType.STANDARD_WITH_LINE_2);
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    addressJson.put("line2", "Apt 123");
    Optional<JsonNode> maybeResp =
        helper.getClient().fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    JsonNode resp = maybeResp.get();

    JsonNode nodeWithLine2 = resp.get("candidates").get(0);
    String actualLine2Value = nodeWithLine2.get("attributes").get("SubAddr").asText();
    assertThat(actualLine2Value).isEqualTo("Apt 123");
  }

  @Test
  public void fetchAddressSuggestionsWithNoCandidates() throws Exception {
    helper = new EsriTestHelper(TestType.NO_CANDIDATES);
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    Optional<JsonNode> maybeResp =
        helper.getClient().fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    JsonNode resp = maybeResp.get();
    ArrayNode candidates = (ArrayNode) resp.get("candidates");
    assertThat(candidates).isEmpty();
  }

  @Test
  public void fetchAddressSuggestionsWithError() throws Exception {
    helper = new EsriTestHelper(TestType.ERROR);
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    Optional<JsonNode> maybeResp =
        helper.getClient().fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    assertThat(maybeResp.isPresent()).isFalse();
  }

  @Test
  public void fetchAddressSuggestionsMultipleUrls() throws Exception {
    helper = new EsriTestHelper(TestType.MULTIPLE_ENDPOINTS);
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    Optional<JsonNode> maybeResp =
        helper.getClient().fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    JsonNode resp = maybeResp.get();
    ArrayNode candidates = (ArrayNode) resp.get("candidates");
    assertThat(resp.get("spatialReference").get("wkid").asInt()).isEqualTo(4326);
    assertThat(candidates).hasSize(8);
  }

  @Test
  public void fetchServiceAreaFeatures() throws Exception {
    helper = new EsriTestHelper(TestType.SERVICE_AREA_VALIDATION);
    Optional<JsonNode> maybeResp =
        helper
            .getClient()
            .fetchServiceAreaFeatures(EsriTestHelper.LOCATION, "/query")
            .toCompletableFuture()
            .join();
    JsonNode resp = maybeResp.get();
    ReadContext ctx = JsonPath.parse(resp.toString());
    List<String> features = ctx.read("features[*].attributes.CITYNAME");
    Optional<String> feature = features.stream().filter(val -> "Seattle".equals(val)).findFirst();
    assertThat(feature.isPresent()).isTrue();
    assertThat(feature.get()).isEqualTo("Seattle");
  }

  @Test
  public void fetchServiceAreaFeaturesWithError() throws Exception {
    helper = new EsriTestHelper(TestType.SERVICE_AREA_VALIDATION_ERROR);
    Optional<JsonNode> maybeResp =
        helper
            .getClient()
            .fetchServiceAreaFeatures(EsriTestHelper.LOCATION, "/query")
            .toCompletableFuture()
            .join();
    assertThat(maybeResp.isPresent()).isFalse();
  }
}
