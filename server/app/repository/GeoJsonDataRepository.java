package repository;

import io.ebean.DB;
import java.util.Optional;
import models.GeoJsonDataModel;

public final class GeoJsonDataRepository {
  public Optional<GeoJsonDataModel> getMostRecentGeoJsonDataRowForEndpoint(String endpoint) {
    return DB.find(GeoJsonDataModel.class)
        .where()
        .eq("endpoint", endpoint)
        .orderBy("createTime desc")
        .setMaxRows(1)
        .findOneOrEmpty();
  }
}
