package durablejobs.jobs;

import durablejobs.DurableJob;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import models.PersistedDurableJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapRefreshJob extends DurableJob {
  private static final Logger logger = LoggerFactory.getLogger(MapRefreshJob.class);

  private final PersistedDurableJobModel persistedDurableJobModel;
  private final Database database;

  public MapRefreshJob(PersistedDurableJobModel persistedDurableJobModel) {
    this.persistedDurableJobModel = persistedDurableJobModel;
    this.database = DB.getDefault();
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJobModel;
  }

  @Override
  public void run() {
    System.out.println("Running MapRefreshJob!");
    logger.info("Starting job to refresh map data.");
    int errorCount = 0;
    try (Transaction jobTransaction = database.beginTransaction()) {
      try {
        logger.info("Try block for refreshing map data.");
      } catch (RuntimeException e) {
        errorCount++;
        logger.error(e.getMessage(), e);
      }

      if (errorCount == 0) {
        System.out.println("Finished MapRefreshJob!");
        logger.info("Finished refreshing map data.");
        jobTransaction.commit();
      } else {
        System.out.println("Error in MapRefreshJob!");
        logger.error(
            "Failed to refresh map data. See previous logs for failures. Total failures: {0}",
            errorCount);
        jobTransaction.rollback();
      }
    }
  }
}
