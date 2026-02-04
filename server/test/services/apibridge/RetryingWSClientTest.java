package services.apibridge;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import repository.ResetPostgres;
import support.WSClientWithHttpHeader;

@RunWith(MockitoJUnitRunner.class)
public class RetryingWSClientTest extends ResetPostgres {
  private static final String GET_URL = "http://mock-web-services:8000/api-bridge/health-check";
  private static final String POST_URL = "http://mock-web-services:8000/api-bridge/bridge/success";
  private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  @Spy
  private RetryAfterHeaderParser retryAfterHeaderParser = instanceOf(RetryAfterHeaderParser.class);

  @AfterClass
  public static void shutdown() {
    scheduler.shutdown();
  }

  private RetryingWSClient createWSClient(Integer emulatedResponseCode) {
    WSClient wsClient = instanceOf(WSClient.class);

    WSClientWithHttpHeader wsClientWithHttpHeader =
        new WSClientWithHttpHeader(wsClient)
            .setHeaders(Map.of("Emulate-Response-Code", List.of(emulatedResponseCode.toString())));

    ApiBridgeExecutionContext executionContext = instanceOf(ApiBridgeExecutionContext.class);

    return new RetryingWSClient(
        wsClientWithHttpHeader, retryAfterHeaderParser, scheduler, executionContext, 1L);
  }

  @Test
  public void successful_get_response_does_not_retry() {
    int expectedHttpResponseCode = 200;
    RetryingWSClient wsClient = spy(createWSClient(expectedHttpResponseCode));
    WSResponse wsResponse = wsClient.get(GET_URL).toCompletableFuture().join();

    assertExpectedMethodsCalled(wsClient, 1, 0);
    assertThat(wsResponse.getStatus()).isEqualTo(expectedHttpResponseCode);
  }

  @Test
  public void successful_post_response_does_not_retry() {
    int expectedHttpResponseCode = 200;
    RetryingWSClient wsClient = spy(createWSClient(expectedHttpResponseCode));
    String jsonBody = "{\"payload\": { \"accountNumber\": 1 }}";
    WSResponse wsResponse = wsClient.post(POST_URL, jsonBody).toCompletableFuture().join();

    assertExpectedMethodsCalled(wsClient, 1, 0);
    assertThat(wsResponse.getStatus()).isEqualTo(expectedHttpResponseCode);
  }

  @Test
  public void http_429_response_retries_using_retry_after_interval() {
    int expectedHttpResponseCode = 429;
    RetryingWSClient wsClient = spy(createWSClient(expectedHttpResponseCode));
    WSResponse wsResponse = wsClient.get(GET_URL).toCompletableFuture().join();

    assertExpectedMethodsCalled(wsClient, 4, 3);
    assertThat(wsResponse.getStatus()).isEqualTo(expectedHttpResponseCode);
  }

  /**
   * This test does not use the mock web services. It differs from the rest in order to check that
   * it doesn't try to parse the `Retry-After` header if it isn't included with the http 429
   * response.
   *
   * <p>This test is heavily mocked to check for this specific case and should not be used as a
   * pattern for the other tests in this class.
   */
  @Test
  public void http_429_response_without_retry_after_header_does_not_user_retry_after_interval() {
    RetryingWSClient retryingWSClient =
        spy(
            new RetryingWSClient(
                mock(WSClient.class),
                retryAfterHeaderParser,
                scheduler,
                instanceOf(ApiBridgeExecutionContext.class),
                1L));

    WSResponse mockResponse = mock(WSResponse.class);
    when(mockResponse.getStatus()).thenReturn(429);
    when(mockResponse.getUri()).thenReturn(URI.create("http://localhost.localdomain"));

    retryingWSClient.handleResponse(mockResponse, 1, () -> null);

    assertExpectedMethodsCalled(retryingWSClient, 1, 0);
  }

  @Test
  public void unsuccessful_response_retries() {
    int expectedHttpResponseCode = 500;
    RetryingWSClient wsClient = spy(createWSClient(expectedHttpResponseCode));
    WSResponse wsResponse = wsClient.get(GET_URL).toCompletableFuture().join();

    assertExpectedMethodsCalled(wsClient, 4, 0);
    assertThat(wsResponse.getStatus()).isEqualTo(expectedHttpResponseCode);
  }

  /**
   * Assert that certain methods are called the number of times we expect them to be called
   *
   * @param wsClient Just the wsClient
   * @param handleResponseCount Number of calls we expect the {@link
   *     RetryingWSClient#handleResponse} to receive. This gets called once when successful or the
   *     retry count + 1.
   * @param retryHeaderParseCount Number of calls we expect the {@link RetryAfterHeaderParser#parse}
   *     to receive. This gets called each attempt only when there is an http 429 with a retry-after
   *     header present.
   */
  private void assertExpectedMethodsCalled(
      RetryingWSClient wsClient, int handleResponseCount, int retryHeaderParseCount) {
    verify(wsClient, times(handleResponseCount)).handleResponse(any(), anyInt(), any());
    verify(retryAfterHeaderParser, times(retryHeaderParseCount)).parse(anyString());
  }
}
