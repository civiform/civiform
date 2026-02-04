package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.DB;
import io.ebean.Database;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import models.GeoJsonDataModel;
import services.geojson.FeatureCollection;
import services.geojson.GeoJsonClient;

public final class GeoJsonDataRepository {
  private final Database database;
  private final DatabaseExecutionContext dbExecutionContext;
  private final TransactionManager transactionManager;
  private static final QueryProfileLocationBuilder queryProfileLocationBuilder =
      new QueryProfileLocationBuilder("GeoJsonDataRepository");

  @Inject
  public GeoJsonDataRepository(DatabaseExecutionContext dbExecutionContext) {
    this.database = DB.getDefault();
    this.dbExecutionContext = checkNotNull(dbExecutionContext);
    this.transactionManager = new TransactionManager();
  }

  public CompletableFuture<Optional<GeoJsonDataModel>> getMostRecentGeoJsonDataRowForEndpoint(
      String endpoint) {
    return supplyAsync(
        () ->
            database
                .find(GeoJsonDataModel.class)
                .setLabel("GeoJsonDataModel.lookupGeoJsonByEndpoint")
                .setProfileLocation(queryProfileLocationBuilder.create("getGeoJsonData"))
                .where()
                .eq("endpoint", endpoint)
                .orderBy("confirmTime desc")
                .setMaxRows(1)
                .findOneOrEmpty(),
        dbExecutionContext);
  }

  public void saveGeoJson(String endpoint, FeatureCollection newGeoJson) {
    transactionManager.execute(
        () -> {
          Optional<GeoJsonDataModel> maybeExistingGeoJsonDataRow =
              getMostRecentGeoJsonDataRowForEndpoint(endpoint).toCompletableFuture().join();

          if (maybeExistingGeoJsonDataRow.isPresent()
              && maybeExistingGeoJsonDataRow.get().getGeoJson().equals(newGeoJson)) {
            updateOldGeoJsonConfirmTime(maybeExistingGeoJsonDataRow.get());
          } else {
            saveNewGeoJson(endpoint, newGeoJson);
          }
        });
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

  public void refreshGeoJson(GeoJsonClient geoJsonClient) {
    transactionManager.execute(
        () -> {
          database
              .find(GeoJsonDataModel.class)
              .setLabel("GeoJsonDataModel.getDistinctEndpoints")
              .setProfileLocation(queryProfileLocationBuilder.create("refreshGeoJson"))
              .select("endpoint")
              .setDistinct(true)
              .findList()
              .forEach(
                  geoJsonData -> {
                    String endpoint = geoJsonData.getEndpoint();
                    geoJsonClient.fetchAndSaveGeoJson(endpoint).toCompletableFuture().join();
                  });
        });
  }
}
