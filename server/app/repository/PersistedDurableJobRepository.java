package repository;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import models.PersistedDurableJob;

/** Implements queries related to {@link PersistedDurableJob}. */
public final class PersistedDurableJobRepository {

  private final Database database;

  public PersistedDurableJobRepository() {
    this.database = DB.getDefault();
  }

  /** All {@link PersistedDurableJob}s ordered by execution time ascending. */
  public ImmutableList<PersistedDurableJob> getJobs() {
    return ImmutableList.copyOf(
        database.find(PersistedDurableJob.class).orderBy("executionTime asc").findList());
  }

  /** Delete all {@link PersistedDurableJob}s that have an execution time older than six months. */
  public int deleteJobsOlderThanSixMonths() {
    return database
        .sqlUpdate(
            "DELETE FROM persisted_durable_jobs WHERE persisted_durable_jobs.execution_time <"
                + " CURRENT_DATE - INTERVAL '6 months'")
        .execute();
  }
}
