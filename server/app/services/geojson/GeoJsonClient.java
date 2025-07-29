package services.geojson;

import static autovalue.shaded.com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.GeoJsonDataModel;
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

  public CompletionStage<FeatureCollection> fetchGeoJson(String endpoint) {
    if (endpoint == null || endpoint.isEmpty()) {
      throw new RuntimeException("Missing geoJsonEndpoint");
    }

    try {
      new URL(endpoint);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Invalid GeoJSON endpoint.");
    }

    return ws.url(endpoint)
        .get()
        .thenApplyAsync(
            res -> {
              int status = res.getStatus();
              String responseBody = res.getBody();

              if (status != 200) {
                logger.error(
                    "GeoJSON fetch failed with status {}: {}", status, res.getStatusText());
                throw new RuntimeException("Failed to fetch GeoJSON: " + status);
              }

              if (responseBody == null || responseBody.isBlank()) {
                logger.error("Empty GeoJSON response for endpoint: {}", endpoint);
                throw new RuntimeException("Empty GeoJSON response");
              }

              try {
                FeatureCollection geoJson =
                    objectMapper.readValue(responseBody, FeatureCollection.class);
                validateGeoJson(geoJson);
                saveGeoJson(endpoint, geoJson);
                return geoJson;
              } catch (JsonProcessingException e) {
                logger.error("Failed to process GeoJSON response", e);
                throw new RuntimeException("Invalid GeoJSON format", e);
              }
            });
  }

  private void validateGeoJson(FeatureCollection geoJson) {
    if (geoJson.features.isEmpty()) {
      throw new RuntimeException("GeoJSON has no features.");
    }

    for (Feature feature : geoJson.features) {
      if (feature.geometry == null || feature.properties == null) {
        throw new RuntimeException("Feature is missing geometry or properties.");
      }
    }
  }

  private void saveGeoJson(String endpoint, FeatureCollection newGeoJson) {
    Optional<GeoJsonDataModel> maybeExistingGeoJsonDataRow =
        geoJsonDataRepository.getMostRecentGeoJsonDataRowForEndpoint(endpoint);

    if (maybeExistingGeoJsonDataRow.isEmpty()) {
      // If no GeoJSON data exists for the endpoint, save a new row
      saveNewGeoJson(endpoint, newGeoJson);
    } else {
      GeoJsonDataModel oldGeoJsonDataRow = maybeExistingGeoJsonDataRow.get();
      if (oldGeoJsonDataRow.getGeoJson().equals(newGeoJson)) {
        // If the old and new GeoJSON is the same, update the row's confirm_time
        updateOldGeoJsonConfirmTime(oldGeoJsonDataRow);
      } else {
        // If the old and new GeoJSON is different, create a new row
        saveNewGeoJson(endpoint, newGeoJson);
      }
    }
  }

  private void updateOldGeoJsonConfirmTime(GeoJsonDataModel geoJsonData) {
    geoJsonData.setConfirmTime(Instant.now());
    geoJsonData.save();
  }

  private void saveNewGeoJson(String endpoint, FeatureCollection geoJson) {
    GeoJsonDataModel geoJsonData = new GeoJsonDataModel();
    geoJsonData.setGeoJson(geoJson);
    geoJsonData.setEndpoint(endpoint);
    geoJsonData.setConfirmTime(Instant.now());
    geoJsonData.save();
  }
}
