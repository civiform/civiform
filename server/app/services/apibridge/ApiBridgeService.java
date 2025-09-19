package services.apibridge;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.apibridge.ApiBridgeServiceDto.BridgeRequest;
import static services.apibridge.ApiBridgeServiceDto.BridgeResponse;
import static services.apibridge.ApiBridgeServiceDto.DiscoveryResponse;
import static services.apibridge.ApiBridgeServiceDto.HealthcheckResponse;
import static services.apibridge.ApiBridgeServiceDto.IProblemDetail;
import static services.apibridge.ApiBridgeServiceDto.ProblemDetail;
import static services.apibridge.ApiBridgeServiceDto.ValidationProblemDetail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import services.ErrorAnd;

/** Handles the direct http communication with a bridge service. */
@Singleton
public final class ApiBridgeService implements WSBodyReadables {
  private static final Logger logger = LoggerFactory.getLogger(ApiBridgeService.class);
  private final WSClient wsClient;
  private final ApiBridgeExecutionContext executionContext;
  private final ObjectMapper mapper;

  private static final String HEALTH_CHECK_PATH = "health-check";
  private static final String DISCOVERY_PATH = "discovery";
  private static final String BRIDGE_ROOT_PATH = "bridge";

  private final Counter requestsTotal =
      Counter.build()
          .name("api_bridge_requests_total")
          .help("Total number of API bridge requests")
          .labelNames("method_name", "status")
          .register();

  private final Histogram requestDuration =
      Histogram.build()
          .name("api_bridge_request_duration_seconds")
          .help("Time spent on API bridge requests in seconds")
          .labelNames("method_name", "status")
          .register();

  @Inject
  public ApiBridgeService(
      WSClient wsClient, ApiBridgeExecutionContext executionContext, ObjectMapper mapper) {
    this.wsClient = checkNotNull(wsClient);
    this.executionContext = checkNotNull(executionContext);
    this.mapper = checkNotNull(mapper);
  }

  /**
   * Calls the health-check endpoint
   *
   * @param hostUrl url to the root of a bridge host
   * @return HealthCheck response or problem detail data
   */
  public CompletionStage<ErrorAnd<HealthcheckResponse, IProblemDetail>> healthcheck(
      String hostUrl) {
    String url = String.format("%s/%s", hostUrl, HEALTH_CHECK_PATH);
    long startTimeNs = System.nanoTime();

    try {
      return wsClient
          .url(url)
          .get()
          .thenApplyAsync(
              res -> {
                ErrorAnd<HealthcheckResponse, IProblemDetail> result =
                    handleResponse(res, HealthcheckResponse.class);
                recordMetrics(HEALTH_CHECK_PATH, result, startTimeNs);
                return result;
              },
              executionContext)
          .exceptionallyAsync(
              ex -> {
                ErrorAnd<HealthcheckResponse, IProblemDetail> result = handleError(url, ex);
                recordErrorMetrics(HEALTH_CHECK_PATH, ex, startTimeNs);
                return result;
              },
              executionContext);
    } catch (RuntimeException ex) {
      ErrorAnd<HealthcheckResponse, IProblemDetail> result = handleError(url, ex);
      recordErrorMetrics(HEALTH_CHECK_PATH, ex, startTimeNs);
      return CompletableFuture.completedFuture(result);
    }
  }

  /**
   * Calls the discovery endpoint
   *
   * @param hostUrl url to the root of a bridge host
   * @return Discovery response or problem detail data
   */
  public CompletionStage<ErrorAnd<DiscoveryResponse, IProblemDetail>> discovery(String hostUrl) {
    String url = String.format("%s/%s", hostUrl, DISCOVERY_PATH);
    long startTimeNs = System.nanoTime();

    try {
      return wsClient
          .url(url)
          .get()
          .thenApplyAsync(
              res -> {
                ErrorAnd<DiscoveryResponse, IProblemDetail> result =
                    handleResponse(res, DiscoveryResponse.class);
                recordMetrics(DISCOVERY_PATH, result, startTimeNs);
                return result;
              },
              executionContext)
          .exceptionallyAsync(
              ex -> {
                ErrorAnd<DiscoveryResponse, IProblemDetail> result = handleError(url, ex);
                recordErrorMetrics(DISCOVERY_PATH, ex, startTimeNs);
                return result;
              },
              executionContext);
    } catch (RuntimeException ex) {
      ErrorAnd<DiscoveryResponse, IProblemDetail> result = handleError(url, ex);
      recordErrorMetrics(DISCOVERY_PATH, ex, startTimeNs);
      return CompletableFuture.completedFuture(result);
    }
  }

  /**
   * Calls the bridge endpoint
   *
   * @param fullBridgeUrl url to the root of a bridge host
   * @param request request body
   * @return Bridge response or problem detail data
   */
  public CompletionStage<ErrorAnd<BridgeResponse, IProblemDetail>> bridge(
      String fullBridgeUrl, BridgeRequest request) {
    long startTimeNs = System.nanoTime();

    try {
      String jsonBody = mapper.writeValueAsString(request);
      logger.debug("URL: {} Body: {}", fullBridgeUrl, jsonBody);

      return wsClient
          .url(fullBridgeUrl)
          .setContentType("application/json")
          .post(jsonBody)
          .thenApplyAsync(
              res -> {
                ErrorAnd<BridgeResponse, IProblemDetail> result =
                    handleResponse(res, BridgeResponse.class);
                recordMetrics(BRIDGE_ROOT_PATH, result, startTimeNs);
                return result;
              },
              executionContext)
          .exceptionallyAsync(
              ex -> {
                ErrorAnd<BridgeResponse, IProblemDetail> result = handleError(fullBridgeUrl, ex);
                recordErrorMetrics(BRIDGE_ROOT_PATH, ex, startTimeNs);
                return result;
              },
              executionContext);
    } catch (RuntimeException | JsonProcessingException ex) {
      ErrorAnd<BridgeResponse, IProblemDetail> result = handleError(fullBridgeUrl, ex);
      recordErrorMetrics(BRIDGE_ROOT_PATH, ex, startTimeNs);
      return CompletableFuture.completedFuture(result);
    }
  }

  /**
   * Handles the raw WSResponse and deal with expected http response codes
   *
   * @param wsResponse an instance of the WSResponse
   * @param classOfT class of the success object result type
   * @return Wraps the result in the ErrorAnd class and returns either the success data class or the
   *     problem details error
   * @param <T> class of the success object result type
   */
  private <T> ErrorAnd<T, IProblemDetail> handleResponse(WSResponse wsResponse, Class<T> classOfT) {
    return switch (wsResponse.getStatus()) {
      case 200 -> ErrorAnd.of(mapper.convertValue(wsResponse.getBody(json()), classOfT));

      case 400, 401, 404, 429, 500 ->
          ErrorAnd.error(
              ImmutableSet.of(
                  mapper.convertValue(wsResponse.getBody(json()), ProblemDetail.class)));

      case 422 ->
          ErrorAnd.error(
              ImmutableSet.of(
                  mapper.convertValue(wsResponse.getBody(json()), ValidationProblemDetail.class)));

      default -> {
        logger.debug(
            "Error calling api bridge: [{} {}] -> {}",
            wsResponse.getStatus(),
            wsResponse.getUri(),
            wsResponse.getBody());
        throw new UnsupportedOperationException(
            String.format(
                "Unsupported response code:%d when calling [%s]",
                wsResponse.getStatus(), wsResponse.getUri()));
      }
    };
  }

  /**
   * Formats a problem detail object when an error occurs
   *
   * @param url Url of the call that failed
   * @param ex Caught exception of the call that failed
   * @return Wraps the error in the ErrorAnd class and returns with problem details error
   * @param <T> class of the success object result type
   */
  private static <T> ErrorAnd<T, IProblemDetail> handleError(String url, Throwable ex) {
    logger.error("Error when calling: {}", url, ex);
    return ErrorAnd.error(
        ImmutableSet.of(
            new ProblemDetail("unhandled-error", "Unhandled Error", 999, ex.getMessage(), url)));
  }

  /** Records metrics for success/error responses */
  private <T> void recordMetrics(
      String methodName, ErrorAnd<T, IProblemDetail> result, long startTimeNs) {
    String status = result.isError() ? "error" : "success";
    double elapsedSeconds = (System.nanoTime() - startTimeNs) / 1_000_000_000.0;

    requestDuration.labels(methodName, status).observe(elapsedSeconds);
    requestsTotal.labels(methodName, status).inc();
  }

  /** Records metrics for exceptions */
  private void recordErrorMetrics(String methodName, Throwable ex, long startTimeNs) {
    String status = ex.getClass().getSimpleName();
    double elapsedSeconds = (System.nanoTime() - startTimeNs) / 1_000_000_000.0;

    requestDuration.labels(methodName, status).observe(elapsedSeconds);
    requestsTotal.labels(methodName, status).inc();
  }
}
