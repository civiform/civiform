package durablejobs.jobs;

import durablejobs.DurableJob;
import models.PersistedDurableJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.GeoJsonDataRepository;
import services.geojson.GeoJsonClient;

public class MapRefreshJob extends DurableJob {
  private static final Logger logger = LoggerFactory.getLogger(MapRefreshJob.class);

  private final PersistedDurableJobModel persistedDurableJobModel;
  private final GeoJsonDataRepository geoJsonDataRepository;
  private final GeoJsonClient geoJsonClient;

  public MapRefreshJob(
      PersistedDurableJobModel persistedDurableJobModel,
      GeoJsonDataRepository geoJsonDataRepository,
      GeoJsonClient geoJsonClient) {
    this.persistedDurableJobModel = persistedDurableJobModel;
    this.geoJsonDataRepository = geoJsonDataRepository;
    this.geoJsonClient = geoJsonClient;
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJobModel;
  }

  @Override
  public void run() {
    logger.info("Starting job to refresh map data.");
    try {
      geoJsonDataRepository.refreshGeoJson(geoJsonClient);
      logger.info("Finished refreshing map data.");
    } catch (RuntimeException e) {
      logger.error("Failed to refresh map data: {}", e.getMessage(), e);
    }
  }
}
