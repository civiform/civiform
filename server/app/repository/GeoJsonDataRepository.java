package repository;

import io.ebean.DB;
import io.ebean.Database;
import java.util.Optional;
import models.GeoJsonDataModel;

public final class GeoJsonDataRepository {

  private final Database database;

  public GeoJsonDataRepository() {
    this.database = DB.getDefault();
  }

  public Optional<GeoJsonDataModel> getMostRecentGeoJsonDataRowForEndpoint(String endpoint) {
    return database
        .find(GeoJsonDataModel.class)
        .where()
        .eq("endpoint", endpoint)
        .orderBy("createTime desc")
        .setMaxRows(1)
        .findOneOrEmpty();
  }
}
