package services.apibridge;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;
import repository.ResetPostgres;

public class FakeApiBridgeServiceTest extends ResetPostgres {
  private static final String BASE_URL = "http://mock-web-services:8000/api-bridge";

  ObjectMapper mapper = new ObjectMapper();

  @Test
  public void healthcheck() throws JsonProcessingException {
    var dispatcher = new FakeApiBridgeService();
    var response = dispatcher.healthcheck(BASE_URL).toCompletableFuture().join();
    var json = mapper.writeValueAsString(response);

    assertThat(response).isNotNull();
    assertThat(response.timestamp()).isGreaterThan(0);

    System.out.println(json);
    System.out.println("--------------------------------------------------------------------");
  }

  @Test
  public void discovery() throws JsonProcessingException {
    var dispatcher = new FakeApiBridgeService();
    var response = dispatcher.discovery(BASE_URL).toCompletableFuture().join();
    var json = mapper.writeValueAsString(response);

    assertThat(response).isNotNull();
    assertThat(response.endpoints()).hasSizeGreaterThan(0);

    System.out.println(json);
    System.out.println("--------------------------------------------------------------------");
  }

  @Test
  public void bridge_success() throws JsonProcessingException, URISyntaxException {
    var dispatcher = new FakeApiBridgeService();

    var request =
        new ApiBridgeServiceDto.BridgeRequest(
            ImmutableMap.of(
                "applicantId",
                "APP-123",
                "householdSize",
                3,
                "annualIncome",
                45000,
                "currentAddress",
                "123 Main St, Anytown, ST 12345"));

    var requestJson = mapper.writeValueAsString(request);

    var response =
        dispatcher
            .bridge(new URI("https://localhost.localdomain/bridge/xxxxxx"), request)
            .toCompletableFuture()
            .join();

    var responseJson = mapper.writeValueAsString(response);

    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    JsonSchema requestSchema =
        factory.getSchema(FakeApiBridgeService.SchemaDatabase.requestSchemaNode());
    JsonSchema responseSchema =
        factory.getSchema(FakeApiBridgeService.SchemaDatabase.responseSchemaNode());

    var validationMessagesRequest =
        requestSchema.validate(mapper.readTree(requestJson).get("payload"));
    var validationMessagesResponse =
        responseSchema.validate(mapper.readTree(responseJson).get("payload"));

    assertThat(validationMessagesRequest.isEmpty()).isTrue();
    assertThat(validationMessagesResponse.isEmpty()).isTrue();
  }
}
