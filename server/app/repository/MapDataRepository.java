package repository;

import io.ebean.DB;
import java.util.Optional;
import models.MapDataModel;

public final class MapDataRepository {
  public Optional<String> getMostRecentGeoJsonForEndpoint(String endpoint) {
    return DB.find(MapDataModel.class)
        .where()
        .eq("endpoint", endpoint)
        .orderBy("updateTime desc")
        .setMaxRows(1)
        .findOneOrEmpty()
        .map(MapDataModel::getGeojson);
  }
}
