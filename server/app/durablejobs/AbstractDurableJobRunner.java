package durablejobs;

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
import javax.inject.Provider;
import models.PersistedDurableJobModel;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.email.aws.SimpleEmail;

/**
 * Executes {@link DurableJob}s when their time has come.
 *
 * <p>AbstractDurableJobRunner children should be singletons and its {@code runJobs} method is
 * {@code synchronized} to prevent overlapping executions within the same server at the same time.
 */
public abstract class AbstractDurableJobRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDurableJobRunner.class);

  private final String hostName;
  private final Database database = DB.getDefault();
  private final DurableJobExecutionContext durableJobExecutionContext;
  private final DurableJobRegistry durableJobRegistry;
  private final String itEmailAddress;
  private final int jobTimeoutMinutes;
  private final Provider<LocalDateTime> nowProvider;
  private final SimpleEmail simpleEmail;
  private final ZoneOffset zoneOffset;

  public AbstractDurableJobRunner(
      Config config,
      DurableJobExecutionContext durableJobExecutionContext,
      DurableJobRegistry durableJobRegistry,
      Provider<LocalDateTime> nowProvider,
      SimpleEmail simpleEmail,
      ZoneId zoneId) {
    this.hostName =
        config.getString("base_url").replace("https", "").replace("http", "").replace("://", "");
    this.durableJobExecutionContext = Preconditions.checkNotNull(durableJobExecutionContext);
    this.durableJobRegistry = Preconditions.checkNotNull(durableJobRegistry);
    this.itEmailAddress =
        config.getString("it_email_address").isBlank()
            ? config.getString("support_email_address")
            : config.getString("it_email_address");
    this.jobTimeoutMinutes = config.getInt("durable_jobs.job_timeout_minutes");

    this.simpleEmail = Preconditions.checkNotNull(simpleEmail);
    this.nowProvider = Preconditions.checkNotNull(nowProvider);
    this.zoneOffset = zoneId.getRules().getOffset(nowProvider.get());
  }

  /** Get the job to run or an empty optional if one does not exist */
  abstract Optional<PersistedDurableJobModel> getJobForExecution();

  /** Determines if the provided job, if it exists, is allowed to be run. */
  abstract boolean canRun(Optional<PersistedDurableJobModel> maybeJobToRun);

  /**
   * Queries for durable jobs that are ready to run and executes them.
   *
   * <p>Continues executing jobs as long as there are jobs to execute and it does not exceed the
   * time specified by "durable_jobs.poll_interval_seconds". This is to prevent runners attempting
   * to run at the same time in the same server.
   *
   * <p>{@code synchronized} to avoid overlapping executions within the same server.
   */
  public synchronized void runJobs() {
    LOGGER.info("JobRunner_Start thread ID={}", Thread.currentThread().getId());

    Transaction transaction = database.beginTransaction();
    Optional<PersistedDurableJobModel> maybeJobToRun = getJobForExecution();

    while (canRun(maybeJobToRun)) {
      PersistedDurableJobModel jobToRun = maybeJobToRun.get();
      runJob(jobToRun);
      notifyUponFinalFailure(jobToRun);
      transaction.commit();

      transaction = database.beginTransaction();
      maybeJobToRun = getJobForExecution();
    }
    transaction.close();

    LOGGER.info("JobRunner_Stop thread_ID={}", Thread.currentThread().getId());
  }

  private void notifyUponFinalFailure(PersistedDurableJobModel job) {
    if (!job.hasFailedWithNoRemainingAttempts()) {
      return;
    }

    String subject = String.format("ERROR: CiviForm Durable job failure on %s", hostName);
    StringBuilder contents = new StringBuilder("A durable job has failed repeatedly on ");
    contents.append(hostName);
    contents.append("\n\n");

    contents.append(
        "This needs to be investigated by IT staff or the CiviForm core team"
            + " (civiform-technical@googlegroups.com).\n\n");
    contents.append(
        String.format("Error report for: job_name=\"%s\", job_ID=%d\n", job.getJobName(), job.id));
    contents.append(job.getErrorMessage().orElse("Job is missing error messages."));

    simpleEmail.send(itEmailAddress, subject, contents.toString());
  }

  private void runJob(PersistedDurableJobModel persistedDurableJob) {
    LocalDateTime startTime = nowProvider.get();
    LOGGER.info(
        "JobRunner_ExecutingJob thread_ID={}, job_name=\"{}\", job_ID={}",
        Thread.currentThread().getId(),
        persistedDurableJob.getJobName(),
        persistedDurableJob.id);

    try {
      persistedDurableJob.decrementRemainingAttempts().save();

      // Run the job in a separate thread and block until it completes, fails, or times out.
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
    } catch (JobNotFoundException e) {
      // If the job is not found in the registry, it was likely removed intentionally
      // In this case, we want to delete the job from the database becuase it should not be run
      // anymore
      if (persistedDurableJob.delete()) {
        LOGGER.info(
            String.format(
                "Job was not found in the registry and was deleted from the db. job_name=\"%s\"",
                persistedDurableJob.getJobName()));
      } else {
        // If the delete fails, handle it like the other errors
        String msg =
            String.format(
                "Job was not found in the registry and there was an error deleting the job. Error:"
                    + " %s job_name=\"%s\", job_ID=%d, attempts_remaining=%d, duration_s=%f,"
                    + " message: %s",
                e.getClass().getSimpleName(),
                persistedDurableJob.getJobName(),
                persistedDurableJob.id,
                persistedDurableJob.getRemainingAttempts(),
                getJobDurationInSeconds(startTime),
                e.getMessage());
        LOGGER.error(msg);
        persistedDurableJob.appendErrorMessage(msg).save();
      }
    } catch (IllegalArgumentException | CancellationException | InterruptedException e) {
      String msg =
          String.format(
              "JobRunner_JobFailed %s job_name=\"%s\", job_ID=%d, attempts_remaining=%d,"
                  + " duration_s=%f,"
                  + " message: %s",
              e.getClass().getSimpleName(),
              persistedDurableJob.getJobName(),
              persistedDurableJob.id,
              persistedDurableJob.getRemainingAttempts(),
              getJobDurationInSeconds(startTime),
              e.getMessage());
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

  private synchronized void runJobWithTimeout(DurableJob jobToRun)
      throws ExecutionException, InterruptedException, TimeoutException {
    CompletableFuture<Void> future =
        CompletableFuture.runAsync(() -> jobToRun.run(), durableJobExecutionContext.current());

    // We set the job timeout to 0 in test
    if (jobTimeoutMinutes == 0) {
      // Timeout test jobs after 2500ms
      future.get(2500, TimeUnit.MILLISECONDS);
      return;
    }

    future.get(jobTimeoutMinutes, TimeUnit.MINUTES);
  }

  private double getJobDurationInSeconds(LocalDateTime startTime) {
    return ((double) ChronoUnit.MILLIS.between(startTime, nowProvider.get())) / 1000;
  }
}
