package services.apibridge;

import static org.assertj.core.api.Assertions.assertThat;
import static services.apibridge.ApiBridgeServiceDto.BridgeRequest;
import static services.apibridge.ApiBridgeServiceDto.BridgeResponse;
import static services.apibridge.ApiBridgeServiceDto.DiscoveryResponse;
import static services.apibridge.ApiBridgeServiceDto.HealthcheckResponse;
import static services.apibridge.ApiBridgeServiceDto.IProblemDetail;
import static services.apibridge.ApiBridgeServiceDto.ProblemDetail;
import static services.apibridge.ApiBridgeServiceDto.ValidationProblemDetail;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.libs.ws.WSClient;
import repository.ResetPostgres;
import services.ErrorAnd;
import support.WSClientWithHttpHeader;

@RunWith(JUnitParamsRunner.class)
public class ApiBridgeServiceTest extends ResetPostgres {
  private static final String BASE_URL = "http://mock-web-services:8000/api-bridge";
  private static final String BRIDGE_URL = String.format("%s/bridge/success", BASE_URL);

  private ApiBridgeService createApiBridgeService() {
    return createApiBridgeService("");
  }

  private ApiBridgeService createApiBridgeService(String emulatedResponseCode) {
    WSClientWithHttpHeader wsClient = new WSClientWithHttpHeader(instanceOf(WSClient.class));

    if (!emulatedResponseCode.isBlank()) {
      wsClient.setHeaders(Map.of("Emulate-Response-Code", List.of(emulatedResponseCode)));
    }

    return new ApiBridgeService(wsClient, instanceOf(ApiBridgeExecutionContext.class));
  }

  /*

  endpoint      | supported http response codes
  --------------|----------------------------------
  health-check  | 200, 400, 401,           429, 500
  discovery     | 200, 400, 401,           429, 500
  bridge/{slug} | 200, 400, 401, 404, 422, 429, 500

  */

  @Test
  public void healthcheck_http_200_returns_success_model() {
    ErrorAnd<HealthcheckResponse, IProblemDetail> response =
        createApiBridgeService().healthcheck(BASE_URL).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isTrue();
    assertThat(response.isError()).isFalse();

    HealthcheckResponse responseModel = response.getResult();
    assertThat(responseModel).isNotNull();
    assertThat(responseModel.timestamp()).isGreaterThan(0);
  }

  @Test
  @Parameters({"400", "401", "429", "500"})
  public void healthcheck_supported_error_response_codes_returns_error_model(
      String emulatedResponseCode) {
    ErrorAnd<HealthcheckResponse, IProblemDetail> response =
        createApiBridgeService(emulatedResponseCode)
            .healthcheck(BASE_URL)
            .toCompletableFuture()
            .join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();

    Optional<IProblemDetail> errorModel = response.getErrors().stream().findFirst();
    assertThat(errorModel.isPresent()).isTrue();
    assertThat(errorModel.get().status().toString()).isEqualTo(emulatedResponseCode);
  }

  @Test
  public void healthcheck_unsupported_response_code_returns_error_model() {
    ErrorAnd<HealthcheckResponse, IProblemDetail> response =
        createApiBridgeService("999").healthcheck(BASE_URL).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();
  }

  @Test
  public void healthcheck_handles_null_url_returns_error_model() {
    ErrorAnd<HealthcheckResponse, IProblemDetail> response =
        createApiBridgeService().healthcheck(null).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();
  }

  @Test
  public void discovery_http_200_returns_success_model() {
    ErrorAnd<DiscoveryResponse, IProblemDetail> response =
        createApiBridgeService().discovery(BASE_URL).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isTrue();
    assertThat(response.isError()).isFalse();

    DiscoveryResponse responseModel = response.getResult();
    assertThat(responseModel.endpoints()).hasSizeGreaterThan(0);
  }

  @Test
  @Parameters({"400", "401", "429", "500"})
  public void discovery_supported_error_response_codes_returns_error_model(
      String emulatedResponseCode) {
    ErrorAnd<DiscoveryResponse, IProblemDetail> response =
        createApiBridgeService(emulatedResponseCode)
            .discovery(BASE_URL)
            .toCompletableFuture()
            .join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();

    Optional<IProblemDetail> errorModel = response.getErrors().stream().findFirst();
    assertThat(errorModel.isPresent()).isTrue();
    assertThat(errorModel.get().status().toString()).isEqualTo(emulatedResponseCode);
  }

  @Test
  public void discovery_unsupported_response_code_returns_error_model() {
    ErrorAnd<DiscoveryResponse, IProblemDetail> response =
        createApiBridgeService("999").discovery(BASE_URL).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();
  }

  @Test
  public void discovery_handles_null_url_returns_error_model() {
    ErrorAnd<DiscoveryResponse, IProblemDetail> response =
        createApiBridgeService().discovery(null).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();
  }

  private BridgeRequest createBridgeRequest() {
    return new BridgeRequest(
        ImmutableMap.<String, Object>builder()
            .put("accountNumber", 1234)
            .put("zipCode", "98056")
            .build());
  }

  @Test
  public void bridge_http_200_returns_success_model() {
    ErrorAnd<BridgeResponse, IProblemDetail> response =
        createApiBridgeService()
            .bridge(BRIDGE_URL, createBridgeRequest())
            .toCompletableFuture()
            .join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isTrue();
    assertThat(response.isError()).isFalse();

    BridgeResponse responseModel = response.getResult();
    assertThat(responseModel).isNotNull();
    assertThat(responseModel.payload().isEmpty()).isFalse();
  }

  @Test
  @Parameters({"400", "401", "404", "429", "500"})
  public void bridge_supported_error_response_codes_returns_error_model(
      String emulatedResponseCode) {
    ErrorAnd<BridgeResponse, IProblemDetail> response =
        createApiBridgeService(emulatedResponseCode)
            .bridge(BRIDGE_URL, createBridgeRequest())
            .toCompletableFuture()
            .join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();

    Optional<IProblemDetail> errorModel = response.getErrors().stream().findFirst();
    assertThat(errorModel.isPresent()).isTrue();
    assertThat(errorModel.get().status().toString()).isEqualTo(emulatedResponseCode);
    assertThat(errorModel.get()).isInstanceOf(ProblemDetail.class);
  }

  @Test
  public void bridge_http_422_error_response_code_returns_validation_error_model() {
    ErrorAnd<BridgeResponse, IProblemDetail> response =
        createApiBridgeService("422")
            .bridge(BRIDGE_URL, createBridgeRequest())
            .toCompletableFuture()
            .join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();

    Optional<IProblemDetail> errorModel = response.getErrors().stream().findFirst();
    assertThat(errorModel.isPresent()).isTrue();
    assertThat(errorModel.get().status().toString()).isEqualTo("422");
    assertThat(errorModel.get()).isInstanceOf(ValidationProblemDetail.class);

    ValidationProblemDetail validationErrorModel = (ValidationProblemDetail) errorModel.get();
    assertThat(validationErrorModel.validationErrors().size()).isEqualTo(2);
  }

  @Test
  public void bridge_unsupported_response_code_returns_error_model() {
    ErrorAnd<BridgeResponse, IProblemDetail> response =
        createApiBridgeService("999")
            .bridge(BRIDGE_URL, createBridgeRequest())
            .toCompletableFuture()
            .join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();
  }

  @Test
  public void bridge_handles_null_host_url_returns_error_model() {
    ErrorAnd<BridgeResponse, IProblemDetail> response =
        createApiBridgeService().bridge(null, createBridgeRequest()).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();
  }

  @Test
  public void bridge_handles_null_bridge_request_returns_error_model() {
    ErrorAnd<BridgeResponse, IProblemDetail> response =
        createApiBridgeService().bridge(BRIDGE_URL, null).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();
  }
}
