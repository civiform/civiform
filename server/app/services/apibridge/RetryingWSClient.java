package services.apibridge;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

/** Decorator class to wrap a {@link WSClient} to add retry capabilities */
public final class RetryingWSClient implements WSClient {
  private static final Logger logger = LoggerFactory.getLogger(RetryingWSClient.class);

  private static final int MAX_RETRIES = 3;
  private static final long DEFAULT_BASE_DELAY_MS = 500L;

  private final WSClient wsClient;
  private final RetryAfterHeaderParser retryAfterHeaderParser;
  private final long baseDelayInMs;
  private final ScheduledExecutorService scheduler;
  private final Executor executionContext;

  @Inject
  public RetryingWSClient(
      WSClient wsClient,
      RetryAfterHeaderParser retryAfterHeaderParser,
      ScheduledExecutorService scheduledExecutorService,
      Executor executionContext) {
    this(
        wsClient,
        retryAfterHeaderParser,
        scheduledExecutorService,
        executionContext,
        DEFAULT_BASE_DELAY_MS);
  }

  @VisibleForTesting
  public RetryingWSClient(
      WSClient wsClient,
      RetryAfterHeaderParser retryAfterHeaderParser,
      ScheduledExecutorService scheduledExecutorService,
      Executor executionContext,
      long baseDelayInMs) {
    this.wsClient = checkNotNull(wsClient);
    this.retryAfterHeaderParser = checkNotNull(retryAfterHeaderParser);
    this.executionContext = checkNotNull(executionContext);
    this.baseDelayInMs = baseDelayInMs;
    this.scheduler = scheduledExecutorService;
  }

  @Override
  public Object getUnderlying() {
    return wsClient.getUnderlying();
  }

  @Override
  public play.api.libs.ws.WSClient asScala() {
    return wsClient.asScala();
  }

  @Override
  public WSRequest url(String url) {
    return wsClient.url(url);
  }

  @Override
  public void close() throws IOException {
    wsClient.close();
  }

  /** Perform a GET on the request asynchronously. */
  public CompletionStage<WSResponse> get(String url) {
    return getWithRetry(url, 0);
  }

  /** Perform a GET on the request asynchronously with retry */
  private CompletionStage<WSResponse> getWithRetry(String url, int retryCount) {
    return this.url(url)
        .get()
        .thenComposeAsync(
            wsResponse ->
                handleResponse(wsResponse, retryCount, () -> getWithRetry(url, retryCount + 1)),
            executionContext)
        .exceptionallyAsync(
            error -> {
              throw new RuntimeException(String.format("Request failed for URL: %s", url), error);
            },
            executionContext);
  }

  /** Perform a POST on the request asynchronously */
  public CompletionStage<WSResponse> post(String url, String jsonBody) {
    return postWithRetry(url, jsonBody, 0);
  }

  /** Perform a POST on the request asynchronously with retry */
  private CompletionStage<WSResponse> postWithRetry(String url, String jsonBody, int retryCount) {
    return this.url(url)
        .setContentType("application/json")
        .post(jsonBody)
        .thenComposeAsync(
            wsResponse ->
                handleResponse(
                    wsResponse, retryCount, () -> postWithRetry(url, jsonBody, retryCount + 1)),
            executionContext)
        .exceptionallyAsync(
            error -> {
              throw new RuntimeException(String.format("Request failed for URL: %s", url), error);
            },
            executionContext);
  }

  /** Handles an HTTP response with retry */
  @VisibleForTesting
  CompletionStage<WSResponse> handleResponse(
      WSResponse wsResponse, int retryCount, Supplier<CompletionStage<WSResponse>> retryAction) {
    // Return immediately if successful http request or max retries exceeded
    if (wsResponse.getStatus() == 200 || retryCount >= MAX_RETRIES) {
      return CompletableFuture.completedFuture(wsResponse);
    }

    // Calculate timeout
    Duration retryDelayDuration = Duration.ofMillis(baseDelayInMs * retryCount);

    if (wsResponse.getStatus() == 429 && wsResponse.getSingleHeader("Retry-After").isPresent()) {
      retryDelayDuration =
          retryAfterHeaderParser.parse(wsResponse.getSingleHeader("Retry-After").get());
    }

    // Schedule retry
    logger.debug(
        "Retrying... URL={} Status={} Delay={}ms Retries={}",
        wsResponse.getUri().toString(),
        wsResponse.getStatus(),
        retryDelayDuration.toMillis(),
        retryCount);

    CompletableFuture<WSResponse> retryFuture = new CompletableFuture<>();

    var unused =
        scheduler.schedule(
            () -> {
              retryAction
                  .get()
                  .whenCompleteAsync(
                      (retryResult, retryError) -> {
                        if (retryError != null) {
                          retryFuture.completeExceptionally(retryError);
                        } else {
                          retryFuture.complete(retryResult);
                        }
                      },
                      executionContext);
            },
            retryDelayDuration.toMillis(),
            TimeUnit.MILLISECONDS);

    return retryFuture;
  }
}
