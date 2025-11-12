package services.geojson;

import static autovalue.shaded.com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.ws.WSClient;
import repository.GeoJsonDataRepository;

public final class GeoJsonClient {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final WSClient ws;
  private final GeoJsonDataRepository geoJsonDataRepository;
  private final ObjectMapper objectMapper;

  @Inject
  public GeoJsonClient(
      WSClient ws, GeoJsonDataRepository geoJsonDataRepository, ObjectMapper objectMapper) {
    this.ws = checkNotNull(ws);
    this.geoJsonDataRepository = geoJsonDataRepository;
    this.objectMapper = checkNotNull(objectMapper);
  }

  /**
   * Method to hit a GeoJSON endpoint and compare a successful response to previously saved data. If
   * the data in the response is different from the stored data, insert a new row in the database;
   * otherwise, update the confirm time of the previously saved data.
   *
   * @param endpoint external endpoint that returns GeoJSON data
   * @return GeoJSON {@link FeatureCollection}
   */
  public CompletionStage<FeatureCollection> fetchAndSaveGeoJson(String endpoint) {
    if (endpoint == null || endpoint.isEmpty()) {
      logger.error("Missing GeoJSON endpoint");
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("Missing GeoJSON endpoint"));
    }

    try {
      URL url = new URL(endpoint);
      url.toURI();

      return ws.url(endpoint)
          .get()
          .thenApplyAsync(
              res -> {
                int status = res.getStatus();
                String responseBody = res.getBody();

                if (status == 401 || status == 403) {
                  logger.error(
                      "GeoJSON fetch failed with status {}: {}", status, res.getStatusText());
                  throw new GeoJsonAccessException("Please provide a publicly accessible URL");
                } else if (status == 404) {
                  logger.error(
                      "GeoJSON fetch failed with status {}: {}", status, res.getStatusText());
                  throw new GeoJsonNotFoundException("Failed to fetch GeoJSON");
                } else if (status != 200) {
                  logger.error(
                      "GeoJSON fetch failed with status {}: {}", status, res.getStatusText());
                  throw new GeoJsonProcessingException("Failed to fetch GeoJSON");
                }

                if (responseBody == null || responseBody.isBlank()) {
                  logger.error("Empty GeoJSON response for endpoint: {}", endpoint);
                  throw new GeoJsonProcessingException("Empty GeoJSON response");
                }

                try {
                  FeatureCollection geoJson =
                      objectMapper.readValue(responseBody, FeatureCollection.class);
                  geoJsonDataRepository.saveGeoJson(endpoint, geoJson);
                  return geoJson;
                } catch (JsonProcessingException e) {
                  logger.error("Failed to process GeoJSON response", e);
                  throw new GeoJsonProcessingException("Invalid GeoJSON format");
                }
              });
    } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
      logger.error("Invalid GeoJSON endpoint: ", e);
      return CompletableFuture.failedFuture(
          new MalformedURLException("Not a valid URL, try retyping"));
    }
  }
}
