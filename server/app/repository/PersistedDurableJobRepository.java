package repository;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import java.time.Instant;
import java.util.Optional;
import javax.inject.Inject;
import models.JobType;
import models.PersistedDurableJobModel;

/** Implements queries related to {@link PersistedDurableJobModel}. */
public final class PersistedDurableJobRepository {
  private static final QueryProfileLocationBuilder queryProfileLocationBuilder =
      new QueryProfileLocationBuilder("PersistedDurableJobRepository");

  private final Database database;

  @Inject
  public PersistedDurableJobRepository() {
    this.database = DB.getDefault();
  }

  /**
   * Find the first scheduled job matching the job name and execution time, or an empty Optional if
   * none exists
   */
  public Optional<PersistedDurableJobModel> findScheduledRecurringJob(String jobName) {
    return database
        .find(PersistedDurableJobModel.class)
        .setLabel("PersistedDurableJobModel.findById")
        .setProfileLocation(queryProfileLocationBuilder.create("findScheduledJob"))
        .where()
        .eq("job_name", jobName)
        .isNull("success_time")
        .gt("remaining_attempts", 0)
        .setMaxRows(1)
        .findOneOrEmpty();
  }

  /** Find the first scheduled job matching the job name or an empty Optional if none exists */
  public Optional<PersistedDurableJobModel> findScheduledJob(String jobName) {
    return database
        .find(PersistedDurableJobModel.class)
        .setLabel("PersistedDurableJobModel.findById")
        .setProfileLocation(queryProfileLocationBuilder.create("findScheduledJob"))
        .where()
        .eq("job_name", jobName)
        .setMaxRows(1)
        .findOneOrEmpty();
  }

  /**
   * Gets a recurring job that is ready to be executed or empty if none are available.
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
  public Optional<PersistedDurableJobModel> getRecurringJobForExecution() {
    return database
        .find(PersistedDurableJobModel.class)
        .forUpdateSkipLocked()
        .setLabel("PersistedDurableJobModel.findById")
        .setProfileLocation(queryProfileLocationBuilder.create("getRecurringJobForExecution"))
        .where()
        .eq("job_type", JobType.RECURRING)
        .le("execution_time", Instant.now())
        .gt("remaining_attempts", 0)
        .isNull("success_time")
        .setMaxRows(1)
        .findOneOrEmpty();
  }

  /**
   * Gets a startup job that is ready to be executed or empty if none are available.
   *
   * <p>A job is ready to be executed if it:
   *
   * <ul>
   *   <li>is not locked for update by another transaction i.e. is not currently being executed
   *       elsewhere
   *   <li>has more than zero remaining attempts
   *   <li>has a null success time (has never succeeded)
   * </ul>
   */
  public Optional<PersistedDurableJobModel> getStartupJobForExecution() {
    return database
        .find(PersistedDurableJobModel.class)
        .forUpdateSkipLocked()
        .setLabel("PersistedDurableJobModel.findById")
        .setProfileLocation(queryProfileLocationBuilder.create("getStartupJobForExecution"))
        .where()
        .in("job_type", JobType.RUN_ONCE, JobType.RUN_ON_EACH_STARTUP)
        .gt("remaining_attempts", 0)
        .isNull("success_time")
        .setMaxRows(1)
        .findOneOrEmpty();
  }

  /** All {@link PersistedDurableJobModel}s ordered by execution time ascending. */
  public ImmutableList<PersistedDurableJobModel> getJobs() {
    return ImmutableList.copyOf(
        database
            .find(PersistedDurableJobModel.class)
            .orderBy("execution_time asc")
            .setLabel("PersistedDurableJobModel.findList")
            .setProfileLocation(queryProfileLocationBuilder.create("getJobs"))
            .findList());
  }

  /**
   * Delete all {@link PersistedDurableJobModel}s that have an execution time older than six months
   * and that are a recurring JobType.
   */
  public int deleteJobsOlderThanSixMonths() {
    return database
        .sqlUpdate(
            """
            DELETE FROM persisted_durable_jobs
            WHERE job_type = 'RECURRING'
            AND execution_time < CURRENT_DATE - INTERVAL '6 months'
            """)
        .execute();
  }
}
