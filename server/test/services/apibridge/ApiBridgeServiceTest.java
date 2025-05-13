package services.apibridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;
import play.libs.ws.WSClient;
import repository.ResetPostgres;

public class ApiBridgeServiceTest extends ResetPostgres {
  private static final String BASE_URL = "http://mock-web-services:8000/api-bridge";

  @Test
  public void healthcheck() {
    var apiBridgeService = new ApiBridgeService(instanceOf(WSClient.class));
    var response = apiBridgeService.healthcheck(BASE_URL).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.timestamp()).isGreaterThan(0);
  }

  @Test
  public void discovery() {
    var apiBridgeService = new ApiBridgeService(instanceOf(WSClient.class));
    var response = apiBridgeService.discovery(BASE_URL).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.endpoints()).hasSizeGreaterThan(0);
  }

  @Test
  public void bridge_success() throws URISyntaxException {
    var apiBridgeService = new ApiBridgeService(instanceOf(WSClient.class));

    var uri = new URI(String.format("%s/bridge/success", BASE_URL));

    var request =
        new ApiBridgeServiceDto.BridgeRequest(
            ImmutableMap.<String, Object>builder()
                .put("accountNumber", 1234)
                .put("zipCode", "98056")
                .build());

    var response = apiBridgeService.bridge(uri, request).toCompletableFuture().join();

    assertThat(response).isNotNull();
    assertThat(response.payload().isEmpty()).isFalse();
  }

  @Test
  public void bridge_fail_validation() throws URISyntaxException {
    var apiBridgeService = new ApiBridgeService(instanceOf(WSClient.class));

    var uri = new URI(String.format("%s/bridge/fail-validation", BASE_URL));

    var request =
        new ApiBridgeServiceDto.BridgeRequest(
            ImmutableMap.<String, Object>builder()
                .put("accountNumber", 1111)
                .put("zipCode", "98056")
                .build());

    assertThatThrownBy(() -> apiBridgeService.bridge(uri, request).toCompletableFuture().join())
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  public void bridge_error() throws URISyntaxException {
    var apiBridgeService = new ApiBridgeService(instanceOf(WSClient.class));

    var uri = new URI(String.format("%s/bridge/error", BASE_URL));

    var request =
        new ApiBridgeServiceDto.BridgeRequest(
            ImmutableMap.<String, Object>builder()
                .put("accountNumber", 1111)
                .put("zipCode", "98056")
                .build());

    assertThatThrownBy(() -> apiBridgeService.bridge(uri, request).toCompletableFuture().join())
        .isInstanceOf(RuntimeException.class);
  }
}
