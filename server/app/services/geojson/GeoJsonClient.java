package services.geojson;

import static autovalue.shaded.com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
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
    this.objectMapper = Preconditions.checkNotNull(objectMapper);
  }

  public CompletionStage<Optional<FeatureCollection>> fetchGeoJsonData(String endpoint) {

    Optional<GeoJsonDataModel> maybeExistingGeoJsonDataRow =
        geoJsonDataRepository.getMostRecentGeoJsonRowForEndpoint(endpoint);

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

          FeatureCollection newGeoJsonData;
          try {
            newGeoJsonData = objectMapper.readValue(res.getBody(), FeatureCollection.class);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
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

          return Optional.of(newGeoJsonData);
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
