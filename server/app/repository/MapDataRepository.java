package repository;

import io.ebean.DB;
import java.util.Optional;
import models.GeoJsonMapDataModel;

public final class MapDataRepository {
  public Optional<String> getMostRecentGeoJsonForEndpoint(String endpoint) {
    return DB.find(GeoJsonMapDataModel.class)
        .where()
        .eq("endpoint", endpoint)
        .orderBy("updateTime desc")
        .setMaxRows(1)
        .findOneOrEmpty()
        .map(GeoJsonMapDataModel::getGeojson);
  }
}
