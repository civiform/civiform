package services.geojson;

import static autovalue.shaded.com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

public class GeoJsonClient {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final WSClient ws;

  @Inject
  public GeoJsonClient(WSClient ws) {
    this.ws = checkNotNull(ws);
  }

  public CompletionStage<Optional<String>> fetchGeoJsonData(String endpoint) {
    WSRequest request = ws.url(endpoint);

    CompletionStage<WSResponse> responsePromise = request.get();
    responsePromise.handle(
        (result, error) -> {
          if (error != null || result.getStatus() != 200) {
            logger.error(
                "GeoJSON API error: {}", error != null ? error.toString() : result.getStatusText());
            return responsePromise;
          } else {
            return CompletableFuture.completedFuture(result);
          }
        });

    return responsePromise.thenApply(
        res -> {
          if (res.getStatus() != 200) {
            return Optional.empty();
          }
          return Optional.of(res.getBody());
        });
  }
}
