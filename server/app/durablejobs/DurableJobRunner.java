package durablejobs;

import annotations.BindingAnnotations;
import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.inject.Provider;
import models.PersistedDurableJob;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.PersistedDurableJobRepository;

/**
 * Executes {@link DurableJob}s when their time has come.
 *
 * <p>The runner continues executing jobs as long as there are jobs to execute and it does not
 * exceed the time specified by "durable_jobs.poll_interval_seconds". This is to prevent runners
 * overlapping in the same server.
 */
public final class DurableJobRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(DurableJobRunner.class);

  private final Database database = DB.getDefault();
  private final DurableJobRegistry durableJobRegistry;
  private final int jobTimeoutMinutes;
  private final PersistedDurableJobRepository persistedDurableJobRepository;
  private final Provider<LocalDateTime> nowProvider;
  private final ZoneOffset zoneOffset;
  private final int runnerLifespanSeconds;

  @Inject
  public DurableJobRunner(
      Config config,
      DurableJobRegistry durableJobRegistry,
      PersistedDurableJobRepository persistedDurableJobRepository,
      @BindingAnnotations.Now Provider<LocalDateTime> nowProvider,
      ZoneId zoneId) {
    this.durableJobRegistry = Preconditions.checkNotNull(durableJobRegistry);
    this.jobTimeoutMinutes = config.getInt("durable_jobs.job_timeout_minutes");
    this.persistedDurableJobRepository = Preconditions.checkNotNull(persistedDurableJobRepository);
    this.runnerLifespanSeconds = config.getInt("durable_jobs.poll_interval_seconds");
    this.nowProvider = Preconditions.checkNotNull(nowProvider);
    this.zoneOffset = zoneId.getRules().getOffset(nowProvider.get());
  }

  public void runJobs() {
    LOGGER.info("JobRunner_Start thread ID={}", Thread.currentThread().getId());

    LocalDateTime stopTime = resolveStopTime();
    Transaction transaction = database.beginTransaction();
    Optional<PersistedDurableJob> jobToRun = persistedDurableJobRepository.getJobForExecution();

    while (jobToRun.isPresent() && nowProvider.get().isBefore(stopTime)) {
      runJob(jobToRun.get());
      transaction.commit();

      transaction = database.beginTransaction();
      jobToRun = persistedDurableJobRepository.getJobForExecution();
    }
    transaction.close();

    LOGGER.info("JobRunner_Stop thread_ID={}", Thread.currentThread().getId());
  }

  private void runJob(PersistedDurableJob persistedDurableJob) {
    LocalDateTime startTime = nowProvider.get();
    LOGGER.info(
        "JobRunner_ExecutingJob thread_ID={}, job_name=\"{}\", job_ID={}",
        Thread.currentThread().getId(),
        persistedDurableJob.getJobName(),
        persistedDurableJob.id);

    try {
      persistedDurableJob.decrementRemainingAttempts().save();

      runJobWithTimeout(
          durableJobRegistry
              .get(DurableJobName.valueOf(persistedDurableJob.getJobName()))
              .getFactory()
              .create(persistedDurableJob));

      persistedDurableJob.setSuccessTime(nowProvider.get().toInstant(zoneOffset)).save();

      LOGGER.info(
          "JobRunner_JobSucceeded job_name=\"{}\", job_ID={}, duration_s={}",
          persistedDurableJob.getJobName(),
          persistedDurableJob.id,
          getJobDurationInSeconds(startTime));
    } catch (JobNotFoundException | CancellationException | InterruptedException e) {
      String msg =
          String.format(
              "JobRunner_JobFailed %s job_name=\"%s\", job_ID=%d, attempts_remaining=%d,"
                  + " duration_s=%f",
              e.getClass(),
              persistedDurableJob.getJobName(),
              persistedDurableJob.id,
              persistedDurableJob.getRemainingAttempts(),
              getJobDurationInSeconds(startTime));
      LOGGER.error(msg);
      persistedDurableJob.appendErrorMessage(msg).save();
    } catch (TimeoutException e) {
      String msg =
          String.format(
              "JobRunner_JobTimeout job_name=\"%s\", job_ID=%d, attempts_remaining=%d,"
                  + " duration_s=%f",
              persistedDurableJob.getJobName(),
              persistedDurableJob.id,
              persistedDurableJob.getRemainingAttempts(),
              getJobDurationInSeconds(startTime));
      LOGGER.error(msg);
      persistedDurableJob.appendErrorMessage(msg).save();
    } catch (ExecutionException e) {
      String msg =
          String.format(
              "JobRunner_JobFailed ExecutionException job_name=\"%s\", job_ID=%d,"
                  + " attempts_remaining=%d, duration_s=%f, error_message=%s, trace=%s",
              persistedDurableJob.getJobName(),
              persistedDurableJob.id,
              persistedDurableJob.getRemainingAttempts(),
              getJobDurationInSeconds(startTime),
              e.getMessage(),
              ExceptionUtils.getStackTrace(e));
      LOGGER.error(msg);
      persistedDurableJob.appendErrorMessage(msg).save();
    }
  }

  private LocalDateTime resolveStopTime() {
    // We set poll interval to 0 in test
    if (runnerLifespanSeconds == 0) {
      // Run for no more than 10
      return nowProvider.get().plus(100, ChronoUnit.MILLIS);
    }

    return nowProvider.get().plus(runnerLifespanSeconds, ChronoUnit.SECONDS);
  }

  private void runJobWithTimeout(DurableJob jobToRun)
      throws ExecutionException, InterruptedException, TimeoutException {
    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> jobToRun.run());

    // We set the job timeout to 0 in test
    if (jobTimeoutMinutes == 0) {
      // Timeout test jobs after 100 milliseconds
      future.get(100, TimeUnit.MILLISECONDS);
      return;
    }

    future.get(jobTimeoutMinutes, TimeUnit.MINUTES);
  }

  private double getJobDurationInSeconds(LocalDateTime startTime) {
    return ((double) ChronoUnit.MILLIS.between(startTime, nowProvider.get())) / 1000;
  }
}
