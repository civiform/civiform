package durablejobs.jobs;

import durablejobs.DurableJob;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import models.PersistedDurableJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.GeoJsonDataRepository;
import services.geojson.GeoJsonClient;

public class MapRefreshJob extends DurableJob {
  private static final Logger logger = LoggerFactory.getLogger(MapRefreshJob.class);

  private final PersistedDurableJobModel persistedDurableJobModel;
  private final Database database;
  private final GeoJsonDataRepository geoJsonDataRepository;
  private final GeoJsonClient geoJsonClient;

  public MapRefreshJob(
      PersistedDurableJobModel persistedDurableJobModel,
      GeoJsonDataRepository geoJsonDataRepository,
      GeoJsonClient geoJsonClient) {
    this.persistedDurableJobModel = persistedDurableJobModel;
    this.geoJsonDataRepository = geoJsonDataRepository;
    this.database = DB.getDefault();
    this.geoJsonClient = geoJsonClient;
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJobModel;
  }

  @Override
  public void run() {
    logger.info("Starting job to refresh map data.");
    int errorCount = 0;
    try (Transaction jobTransaction = database.beginTransaction()) {
      try {
        geoJsonDataRepository.refreshGeoJson(geoJsonClient);
      } catch (RuntimeException e) {
        errorCount++;
        logger.error(e.getMessage(), e);
      }

      if (errorCount == 0) {
        logger.info("Finished refreshing map data.");
        jobTransaction.commit();
      } else {
        logger.error(
            "Failed to refresh map data. See previous logs for failures. Total failures: {0}",
            errorCount);
        jobTransaction.rollback();
      }
    }
  }
}
