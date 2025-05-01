package repository;

import io.ebean.DB;
import io.ebean.Database;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Implements a simple query needed to check if DB is reachable */
public final class HealthCheckRepository {
  private final Database database;
  private final Logger logger = LoggerFactory.getLogger(HealthCheckRepository.class);

  @Inject
  public HealthCheckRepository() {
    this.database = DB.getDefault();
  }

  /**
   * Does a simple query to check if the DB is reachable. In case of any exception, error is logged
   * and false is returned.
   */
  public boolean isDBReachable() {
    try {
      return database.sqlQuery("SELECT 1").findOne() != null;
    } catch (RuntimeException e) {
      logger.error(e.getMessage());
    }
    return false;
  }
}
