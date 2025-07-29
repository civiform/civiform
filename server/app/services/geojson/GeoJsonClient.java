package services.geojson;

import static autovalue.shaded.com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.GeoJsonDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
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

  private CompletionStage<FeatureCollection> fetchGeoJsonData(String endpoint) {
    // Request GeoJSON data from admin provided endpoint
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

    return responsePromise.thenCompose(
        res -> {
          FeatureCollection geoJsonResponse;

          try {
            geoJsonResponse = objectMapper.readValue(res.getBody(), FeatureCollection.class);
          } catch (JsonProcessingException e) {
            logger.error("Unable to parse GeoJSON from response", e);
            throw new RuntimeException(e);
          }

          if (geoJsonResponse.features.isEmpty()) {
            throw new RuntimeException("No GeoJSON response.");
          }

          return CompletableFuture.completedFuture(geoJsonResponse);
        });
  }

  public CompletionStage<FeatureCollection> getGeoJsonData(String endpoint) {

    Optional<GeoJsonDataModel> maybeExistingGeoJsonDataRow =
        geoJsonDataRepository.getMostRecentGeoJsonRowForEndpoint(endpoint);

    return fetchGeoJsonData(endpoint)
        .thenApply(
            newGeoJsonData -> {
              if (maybeExistingGeoJsonDataRow.isEmpty()) {
                saveData(endpoint, newGeoJsonData);
              } else {
                GeoJsonDataModel oldGeoJsonRow = maybeExistingGeoJsonDataRow.get();
                FeatureCollection oldGeoJsonData = oldGeoJsonRow.getGeoJson();
                if (oldGeoJsonData.equals(newGeoJsonData)) {
                  updateConfirmTime(oldGeoJsonRow);
                } else {
                  saveData(endpoint, newGeoJsonData);
                }
              }

              return newGeoJsonData;
            });
  }

  private void updateConfirmTime(GeoJsonDataModel geoJsonData) {
    geoJsonData.setConfirmTime(Instant.now());
    geoJsonData.save();
  }

  private void saveData(String endpoint, FeatureCollection geoJson) {
    GeoJsonDataModel geoJsonData = new GeoJsonDataModel();
    geoJsonData.setGeoJson(geoJson);
    geoJsonData.setEndpoint(endpoint);
    geoJsonData.setConfirmTime(Instant.now());
    geoJsonData.save();
  }
}
