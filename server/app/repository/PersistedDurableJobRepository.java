package repository;

import annotations.BindingAnnotations;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import models.PersistedDurableJob;

/** Implements queries related to {@link PersistedDurableJob}. */
public final class PersistedDurableJobRepository {

  private final Database database;
  private final Provider<LocalDateTime> nowProvider;

  @Inject
  public PersistedDurableJobRepository(
      @BindingAnnotations.Now Provider<LocalDateTime> nowProvider) {
    this.database = DB.getDefault();
    this.nowProvider = Preconditions.checkNotNull(nowProvider);
  }

  public Optional<PersistedDurableJob> findScheduledJob(String jobName, Instant executionTime) {
    return database
        .find(PersistedDurableJob.class)
        .where()
        .eq("job_name", jobName)
        .eq("execution_time", executionTime)
        .setMaxRows(1)
        .findOneOrEmpty();
  }

  /**
   * Gets a job that is ready to be executed or empty if none are available.
   *
   * <p>A job is ready to be executed if it:
   *
   * <ul>
   *   <li>is not locked for update by another transaction i.e. is not currently being executed
   *       elsewhere
   *   <li>has more than zero remaining attempts
   *   <li>has an execution time is now or in the past
   *   <li>has a null success time (has never succeeded)
   * </ul>
   */
  public Optional<PersistedDurableJob> getJobForExecution() {
    return database
        .find(PersistedDurableJob.class)
        .forUpdateSkipLocked()
        .where()
        .le("execution_time", nowProvider.get())
        .gt("remaining_attempts", 0)
        .isNull("success_time")
        .setMaxRows(1)
        .findOneOrEmpty();
  }

  /** All {@link PersistedDurableJob}s ordered by execution time ascending. */
  public ImmutableList<PersistedDurableJob> getJobs() {
    return ImmutableList.copyOf(
        database.find(PersistedDurableJob.class).orderBy("execution_time asc").findList());
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
