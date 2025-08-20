package services.apibridge;

import static org.assertj.core.api.Assertions.assertThat;
import static services.apibridge.ApiBridgeServiceDto.BridgeRequest;
import static services.apibridge.ApiBridgeServiceDto.BridgeResponse;
import static services.apibridge.ApiBridgeServiceDto.DiscoveryResponse;
import static services.apibridge.ApiBridgeServiceDto.HealthcheckResponse;
import static services.apibridge.ApiBridgeServiceDto.IProblemDetail;
import static services.apibridge.ApiBridgeServiceDto.ProblemDetail;
import static services.apibridge.ApiBridgeServiceDto.ValidationProblemDetail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
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

    return new ApiBridgeService(
        wsClient, instanceOf(ApiBridgeExecutionContext.class), instanceOf(ObjectMapper.class));
  }

  @Before
  public void setup() {
    // Make sure to clear any prometheus metrics between tests
    CollectorRegistry.defaultRegistry.clear();
  }

  /*

  endpoint      | supported http response codes
  --------------|----------------------------------
  health-check  | 200, 400, 401,           429, 500
  discovery     | 200, 400, 401,           429, 500
  bridge/{slug} | 200, 400, 401, 404, 422, 429, 500

  */

  @Test
  public void healthcheck_http200_returnsSuccessModel() {
    ErrorAnd<HealthcheckResponse, IProblemDetail> response =
        createApiBridgeService().healthcheck(BASE_URL).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isTrue();
    assertThat(response.isError()).isFalse();

    HealthcheckResponse responseModel = response.getResult();
    assertThat(responseModel).isNotNull();
    assertThat(responseModel.timestamp()).isGreaterThan(0);
    assertMetricsSuccess("health-check");
  }

  @Test
  @Parameters({"400", "401", "429", "500"})
  public void healthcheck_supportedErrorResponseCodes_returnsErrorModel(
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
    assertMetricsHandledError("health-check");
  }

  @Test
  public void healthcheck_unsupportedResponseCode_returnsErrorModel() {
    ErrorAnd<HealthcheckResponse, IProblemDetail> response =
        createApiBridgeService("999").healthcheck(BASE_URL).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();
    assertMetricsUnhandledError("health-check");
  }

  @Test
  public void healthcheck_handlesNullUrl_returnsErrorModel() {
    ErrorAnd<HealthcheckResponse, IProblemDetail> response =
        createApiBridgeService().healthcheck(null).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();
    assertMetricsUnhandledError("health-check");
  }

  @Test
  public void discovery_http200_returnsSuccessModel() {
    ErrorAnd<DiscoveryResponse, IProblemDetail> response =
        createApiBridgeService().discovery(BASE_URL).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isTrue();
    assertThat(response.isError()).isFalse();

    DiscoveryResponse responseModel = response.getResult();
    assertThat(responseModel.endpoints()).hasSizeGreaterThan(0);
    assertMetricsSuccess("discovery");
  }

  @Test
  @Parameters({"400", "401", "429", "500"})
  public void discovery_supportedErrorResponseCodes_returnsErrorModel(String emulatedResponseCode) {
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
    assertMetricsHandledError("discovery");
  }

  @Test
  public void discovery_unsupportedResponseCode_returnsErrorModel() {
    ErrorAnd<DiscoveryResponse, IProblemDetail> response =
        createApiBridgeService("999").discovery(BASE_URL).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();
    assertMetricsUnhandledError("discovery");
  }

  @Test
  public void discovery_handlesNullUrl_returnsErrorModel() {
    ErrorAnd<DiscoveryResponse, IProblemDetail> response =
        createApiBridgeService().discovery(null).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();
    assertMetricsUnhandledError("discovery");
  }

  private BridgeRequest createBridgeRequest() {
    return new BridgeRequest(
        ImmutableMap.<String, Object>builder()
            .put("accountNumber", 1234)
            .put("zipCode", "98056")
            .build());
  }

  @Test
  public void bridge_http200_returnsSuccessModel() {
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
    assertMetricsSuccess("bridge");
  }

  @Test
  @Parameters({"400", "401", "404", "429", "500"})
  public void bridge_supportedErrorResponseCodes_returnsErrorModel(String emulatedResponseCode) {
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
    assertMetricsHandledError("bridge");
  }

  @Test
  public void bridge_http422ErrorResponseCode_returnsValidationErrorModel() {
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
    assertMetricsHandledError("bridge");
  }

  @Test
  public void bridge_unsupportedResponseCode_returnsErrorModel() {
    ErrorAnd<BridgeResponse, IProblemDetail> response =
        createApiBridgeService("999")
            .bridge(BRIDGE_URL, createBridgeRequest())
            .toCompletableFuture()
            .join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();
    assertMetricsUnhandledError("bridge");
  }

  @Test
  public void bridge_handlesNullHostUrl_returnsErrorModel() {
    ErrorAnd<BridgeResponse, IProblemDetail> response =
        createApiBridgeService().bridge(null, createBridgeRequest()).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();
    assertMetricsUnhandledError("bridge");
  }

  @Test
  public void bridge_handlesNullBridgeRequest_returnsErrorModel() {
    ErrorAnd<BridgeResponse, IProblemDetail> response =
        createApiBridgeService().bridge(BRIDGE_URL, null).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.hasResult()).isFalse();
    assertThat(response.isError()).isTrue();
    assertMetricsHandledError("bridge");
  }

  private void assertMetricsSuccess(String methodName) {
    assertPrometheusMetric("api_bridge_requests_total", methodName, "success");
    assertPrometheusMetric("api_bridge_request_duration_seconds_sum", methodName, "success");
  }

  private void assertMetricsHandledError(String methodName) {
    assertPrometheusMetric("api_bridge_requests_total", methodName, "error");
    assertPrometheusMetric("api_bridge_request_duration_seconds_sum", methodName, "error");
  }

  private void assertMetricsUnhandledError(String methodName) {
    assertPrometheusMetricForUnhandledErrors("api_bridge_requests_total", methodName);
    assertPrometheusMetricForUnhandledErrors("api_bridge_request_duration_seconds_sum", methodName);
  }

  /**
   * Asserts that the prometheus metric for the specific method label is from the specified handled
   * success/error and has a non-zero sample size.
   */
  private void assertPrometheusMetric(String metricName, String methodName, String status) {
    Optional<Collector.MetricFamilySamples.Sample> optionalSample =
        Collections.list(CollectorRegistry.defaultRegistry.metricFamilySamples()).stream()
            .flatMap(mfs -> mfs.samples.stream())
            .filter(sample -> sample.name.equals(metricName))
            .findFirst();

    assertThat(optionalSample.isPresent()).isTrue();

    Collector.MetricFamilySamples.Sample durationSample = optionalSample.get();

    assertThat(durationSample.labelValues)
        .isNotNull()
        .containsExactlyInAnyOrder(new String[] {methodName, status});

    assertThat(durationSample.value).isGreaterThan(0.0);
  }

  /**
   * Asserts that the prometheus metric for the specific method label has a non-zero sample size and
   * verifies the unhandled exception is not recorded as success or error.
   */
  private void assertPrometheusMetricForUnhandledErrors(String metricName, String methodName) {
    Optional<Collector.MetricFamilySamples.Sample> optionalSample =
        Collections.list(CollectorRegistry.defaultRegistry.metricFamilySamples()).stream()
            .flatMap(mfs -> mfs.samples.stream())
            .filter(sample -> sample.name.equals(metricName))
            .findFirst();

    assertThat(optionalSample.isPresent()).isTrue();

    Collector.MetricFamilySamples.Sample durationSample = optionalSample.get();

    assertThat(durationSample.labelValues)
        .isNotNull()
        .hasSize(2)
        .contains(methodName)
        .satisfies(
            list ->
                assertThat(
                        list.stream().filter(s -> !s.equals(methodName)).findFirst().orElseThrow())
                    .isNotEqualTo("success")
                    .isNotEqualTo("error"));

    assertThat(durationSample.value).isGreaterThan(0.0);
  }
}
