package repository;

import io.ebean.DB;
import io.ebean.Database;
import io.ebean.SqlRow;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HealthCheckRepository {
  private final Database database;
  private final Logger log = LoggerFactory.getLogger(HealthCheckRepository.class);

  @Inject
  public HealthCheckRepository() {
    this.database = DB.getDefault();
  }

  public Optional<SqlRow> checkDBHealth() {
    Optional<SqlRow> result = Optional.empty();
    try {
      result = database.sqlQuery("SELECT 1").findOneOrEmpty();
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return result;
  }
}
