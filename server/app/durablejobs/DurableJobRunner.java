package durablejobs;

import annotations.BindingAnnotations;
import com.google.common.base.Preconditions;
import io.ebean.DB;
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

/** Executes {@link DurableJob}s when their time has come. */
public final class DurableJobRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(DurableJobRunner.class);

  private final DurableJobRegistry durableJobRegistry;
  private final PersistedDurableJobRepository persistedDurableJobRepository;
  private final Provider<LocalDateTime> nowProvider;
  private final ZoneOffset zoneOffset;

  @Inject
  public DurableJobRunner(
      DurableJobRegistry durableJobRegistry,
      PersistedDurableJobRepository persistedDurableJobRepository,
      @BindingAnnotations.Now Provider<LocalDateTime> nowProvider,
      ZoneId zoneId) {
    this.durableJobRegistry = Preconditions.checkNotNull(durableJobRegistry);
    this.persistedDurableJobRepository = Preconditions.checkNotNull(persistedDurableJobRepository);
    this.nowProvider = Preconditions.checkNotNull(nowProvider);
    this.zoneOffset = zoneId.getRules().getOffset(nowProvider.get());
  }

  public void runJobs() {
    LOGGER.info("JobRunner_Start thread ID={}", Thread.currentThread().getId());

    // TODO: extract to config var
    LocalDateTime stopTime = nowProvider.get().plusSeconds(10);
    Optional<PersistedDurableJob> jobToRun;

    do {
      try (Transaction transaction = DB.beginTransaction()) {
        jobToRun = persistedDurableJobRepository.getJobForExecution();
        runJob(jobToRun.get());
      }

      jobToRun = persistedDurableJobRepository.getJobForExecution();
    } while (jobToRun.isPresent() && nowProvider.get().isBefore(stopTime));

    LOGGER.info("JobRunner_Stop thread ID={}", Thread.currentThread().getId());
  }

  private void runJob(PersistedDurableJob persistedDurableJob) {
    LocalDateTime startTime = nowProvider.get();
    LOGGER.info(
        "JobRunner_ExecutingJob thread ID={}, job name=\"{}\", job ID={}",
        Thread.currentThread().getId(),
        persistedDurableJob.getJobName(),
        persistedDurableJob.id);

    try {
      DurableJobRegistry.RegisteredJob jobToRun =
          durableJobRegistry.get(DurableJobName.valueOf(persistedDurableJob.getJobName()));

      persistedDurableJob.decrementRemainingAttempts();
      CompletableFuture.runAsync(() -> jobToRun.getFactory().create(persistedDurableJob).run())
        // TODO: extract to config var
          .get(30, TimeUnit.MINUTES);

      persistedDurableJob.setSuccessTime(nowProvider.get().toInstant(zoneOffset)).save();

      LOGGER.info(
          "JobRunner_JobSucceeded job name=\"{}\", job ID={}, duration (s)={}",
          persistedDurableJob.getJobName(),
          persistedDurableJob.id,
          getJobDurationInSeconds(startTime));
      // TODO: convert all error logs to String.format and save to
      // PersistedDurableJob.appendErrorMessage in addition to logging
    } catch (JobNotFoundException e) {
      LOGGER.error(
          "JobRunner_JobFailed JobNotFoundException job name=\"{}\", job ID={}, attempts"
              + " remaining={}",
          persistedDurableJob.getJobName(),
          persistedDurableJob.id,
          persistedDurableJob.getRemainingAttempts());
    } catch (CancellationException | InterruptedException e) {
      LOGGER.error(
          "JobRunner_JobFailed {} job name=\"{}\", job ID={}, attempts remaining={}",
          e.getClass().toString(),
          persistedDurableJob.getJobName(),
          persistedDurableJob.id,
          persistedDurableJob.getRemainingAttempts());
    } catch (ExecutionException e) {
      LOGGER.error(
          "JobRunner_JobFailed ExecutionException job name=\"{}\", job ID={}, attempts"
              + " remaining={}, error message={}, trace=",
          persistedDurableJob.getJobName(),
          persistedDurableJob.id,
          persistedDurableJob.getRemainingAttempts(),
          e.getMessage(),
          ExceptionUtils.getStackTrace(e));
    } catch (TimeoutException e) {
      LOGGER.error(
          "JobRunner_JobTimeout job name=\"{}\", job ID={}, attempts remaining={}, duration (s)={}",
          persistedDurableJob.getJobName(),
          persistedDurableJob.id,
          persistedDurableJob.getRemainingAttempts(),
          getJobDurationInSeconds(startTime));
    }
  }

  private double getJobDurationInSeconds(LocalDateTime startTime) {
    return ((double) ChronoUnit.MILLIS.between(startTime, nowProvider.get())) / 1000;
  }
}
