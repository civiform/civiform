package repository;

import com.google.common.annotations.VisibleForTesting;
import io.ebean.DB;
import java.time.Instant;
import java.util.Optional;
import models.GeoJsonDataModel;
import services.geojson.FeatureCollection;

public final class GeoJsonDataRepository {
  public Optional<GeoJsonDataModel> getMostRecentGeoJsonDataRowForEndpoint(String endpoint) {
    return DB.find(GeoJsonDataModel.class)
        .where()
        .eq("endpoint", endpoint)
        .orderBy("createTime desc")
        .setMaxRows(1)
        .findOneOrEmpty();
  }

  public void saveGeoJson(String endpoint, FeatureCollection newGeoJson) {
    Optional<GeoJsonDataModel> maybeExistingGeoJsonDataRow =
        getMostRecentGeoJsonDataRowForEndpoint(endpoint);

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
