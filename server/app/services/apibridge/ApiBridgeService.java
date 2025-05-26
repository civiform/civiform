package services.apibridge;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.inject.Inject;
import java.net.URI;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSBodyWritables;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

public class ApiBridgeService implements WSBodyReadables, WSBodyWritables, IApiBridgeService {
  private static final ObjectMapper mapper = new ObjectMapper().registerModule(new GuavaModule());
  private static final Logger logger = LoggerFactory.getLogger(ApiBridgeService.class);
  private final WSClient wsClient;

  @Inject
  public ApiBridgeService(WSClient wsClient) {
    this.wsClient = checkNotNull(wsClient);
  }

  @Override
  public CompletionStage<ApiBridgeServiceDto.HealthcheckResponse> healthcheck(String hostUri) {
    return wsClient
        .url(String.format("%s/health-check", hostUri))
        .get()
        .thenApply(wsResponse -> apply(wsResponse, ApiBridgeServiceDto.HealthcheckResponse.class));
  }

  @Override
  public CompletionStage<ApiBridgeServiceDto.DiscoveryResponse> discovery(String hostUri) {
    return wsClient
        .url(String.format("%s/discovery", hostUri))
        .get()
        .thenApply(wsResponse -> apply(wsResponse, ApiBridgeServiceDto.DiscoveryResponse.class));
  }

  @Override
  public CompletionStage<ApiBridgeServiceDto.BridgeResponse> bridge(
      URI uri, ApiBridgeServiceDto.BridgeRequest request) {
    String jsonBody = toJsonString(request);
    logger.info(uri.toString());
    logger.info(jsonBody);

    return wsClient
        .url(uri.toString())
        .addHeader("Content-Type", "application/json")
        .post(jsonBody)
        .thenApply(wsResponse -> apply(wsResponse, ApiBridgeServiceDto.BridgeResponse.class));
  }

  private String toJsonString(Object object) {
    try {
      return mapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private <T> T apply(WSResponse wsResponse, Class<T> classOfT) {
    if (wsResponse.getStatus() != 200) {
      throw new RuntimeException(
          String.format(
              "Error calling discovery: [%s] %s", wsResponse.getUri(), wsResponse.getBody()));
    }

    return mapper.convertValue(wsResponse.getBody(json()), classOfT);
  }
}
