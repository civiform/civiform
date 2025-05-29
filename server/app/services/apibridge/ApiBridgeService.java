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
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import services.ErrorAnd;

/** Handles the direct http communication with a bridge service. */
public class ApiBridgeService implements WSBodyReadables {
  private static final ObjectMapper mapper = new ObjectMapper().registerModule(new GuavaModule());
  private static final Logger logger = LoggerFactory.getLogger(ApiBridgeService.class);
  private final WSClient wsClient;
  private final ApiBridgeExecutionContext executionContext;
  private static final String HEALTHCHECK_PATH = "/health-check";
  private static final String DISCOVERY_PATH = "/discovery";

  @Inject
  public ApiBridgeService(WSClient wsClient, ApiBridgeExecutionContext executionContext) {
    this.wsClient = checkNotNull(wsClient);
    this.executionContext = checkNotNull(executionContext);
  }

  /**
   * Calls the health-check endpoint
   *
   * @param hostUrl url to the root of a bridge host
   * @return HealthCheck response or problem detail data
   */
  public CompletionStage<ErrorAnd<HealthcheckResponse, IProblemDetail>> healthcheck(
      String hostUrl) {
    String url = String.format("%s/%s", hostUrl, HEALTHCHECK_PATH);
    try {
      return wsClient
          .url(url)
          .get()
          .thenApplyAsync(res -> handleResponse(res, HealthcheckResponse.class), executionContext)
          .exceptionallyAsync(ex -> handleError(url, ex), executionContext);
    } catch (RuntimeException ex) {
      return CompletableFuture.completedFuture(handleError(url, ex));
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
    try {
      return wsClient
          .url(url)
          .get()
          .thenApplyAsync(res -> handleResponse(res, DiscoveryResponse.class), executionContext)
          .exceptionallyAsync(ex -> handleError(url, ex), executionContext);
    } catch (RuntimeException ex) {
      return CompletableFuture.completedFuture(handleError(url, ex));
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
    try {
      String jsonBody = toJsonString(request);
      logger.debug("URL: {} Body: {}", fullBridgeUrl, jsonBody);

      return wsClient
          .url(fullBridgeUrl)
          .setContentType("application/json")
          .post(jsonBody)
          .thenApplyAsync(res -> handleResponse(res, BridgeResponse.class), executionContext)
          .exceptionallyAsync(ex -> handleError(fullBridgeUrl, ex), executionContext);
    } catch (RuntimeException ex) {
      return CompletableFuture.completedFuture(handleError(fullBridgeUrl, ex));
    }
  }

  /** Convert object to JSON without having to use stupid checked exceptions */
  private String toJsonString(Object object) {
    try {
      return mapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
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
}
