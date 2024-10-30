package durablejobs;

import annotations.BindingAnnotations;
import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import models.PersistedDurableJobModel;
import repository.PersistedDurableJobRepository;
import services.email.EmailSendClient;

/**
 * Executes {@link DurableJob}s when their time has come.
 *
 * <p>{@link RecurringDurableJobRunner} is a singleton and its {@code runJobs} method is {@code
 * synchronized} to prevent overlapping executions within the same server at the same time.
 */
@Singleton
public final class RecurringDurableJobRunner extends AbstractDurableJobRunner {
  private final PersistedDurableJobRepository persistedDurableJobRepository;
  private final Provider<LocalDateTime> nowProvider;
  private final int runnerLifespanSeconds;

  @Inject
  public RecurringDurableJobRunner(
      Config config,
      DurableJobExecutionContext durableJobExecutionContext,
      @BindingAnnotations.RecurringJobsProviderName DurableJobRegistry durableJobRegistry,
      PersistedDurableJobRepository persistedDurableJobRepository,
      @BindingAnnotations.Now Provider<LocalDateTime> nowProvider,
      EmailSendClient emailSendClient,
      ZoneId zoneId) {
    super(
        config,
        durableJobExecutionContext,
        durableJobRegistry,
        nowProvider,
        emailSendClient,
        zoneId);
    this.persistedDurableJobRepository = Preconditions.checkNotNull(persistedDurableJobRepository);
    this.nowProvider = Preconditions.checkNotNull(nowProvider);
    this.runnerLifespanSeconds = config.getInt("durable_jobs.poll_interval_seconds");
  }

  /** Get the job to run or an empty optional if one does not exist */
  @Override
  protected synchronized Optional<PersistedDurableJobModel> getJobForExecution() {
    return persistedDurableJobRepository.getRecurringJobForExecution();
  }

  /** Determines if the provided job, if it exists, is allowed to be run. */
  @Override
  protected synchronized boolean canRun(Optional<PersistedDurableJobModel> maybeJobToRun) {
    LocalDateTime stopTime = resolveStopTime();
    return maybeJobToRun.isPresent() && nowProvider.get().isBefore(stopTime);
  }

  private synchronized LocalDateTime resolveStopTime() {
    // We set poll interval to 0 in test
    if (runnerLifespanSeconds == 0) {
      // Run for no more than 5 seconds
      return nowProvider.get().plus(5000, ChronoUnit.MILLIS);
    }

    return nowProvider.get().plus(runnerLifespanSeconds, ChronoUnit.SECONDS);
  }
}
