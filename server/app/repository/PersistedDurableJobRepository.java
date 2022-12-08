package repository;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import javax.inject.Inject;
import models.PersistedDurableJob;

/** Implements queries related to {@link PersistedDurableJob}. */
public final class PersistedDurableJobRepository {

  private final Database database;

  @Inject
  public PersistedDurableJobRepository() {
    this.database = DB.getDefault();
  }

  public ImmutableList<PersistedDurableJob> listJobs() {
    return ImmutableList.copyOf(database.find(PersistedDurableJob.class).findList());
  }

  public int deleteJobsOlderThanSixMonths() {
    return database
        .sqlUpdate(
            "DELETE FROM persisted_durable_jobs WHERE persisted_durable_jobs.execution_time <"
                + " CURRENT_DATE - INTERVAL '6 months'")
        .execute();
  }
}
