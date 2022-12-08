package repository;

import com.google.common.base.Preconditions;
import io.ebean.Database;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PersistedDurableJobRepository {
  private static final Logger logger = LoggerFactory.getLogger(PersistedDurableJobRepository.class);

  private final Database database;

  public PersistedDurableJobRepository(Database database) {
    this.database = Preconditions.checkNotNull(database);
  }

  public void deleteJobsOlderThan(Instant cullingCutoff) {}
}
