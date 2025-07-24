package repository;

import io.ebean.DB;
import java.util.Optional;
import models.GeoJsonDataModel;

public final class GeoJsonDataRepository {
  public Optional<String> getMostRecentGeoJsonForEndpoint(String endpoint) {
    return DB.find(GeoJsonDataModel.class)
        .where()
        .eq("endpoint", endpoint)
        .orderBy("updateTime desc")
        .setMaxRows(1)
        .findOneOrEmpty()
        .map(GeoJsonDataModel::getGeoJson);
  }
}
