package durablejobs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import javax.persistence.OptimisticLockException;
import models.JobType;
import models.PersistedDurableJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.PersistedDurableJobRepository;

/**
 * Schedules recurring jobs in the future.
 *
 * <p>RecurringJobScheduler is a singleton and its methods are {@code synchronized} to prevent
 * overlapping executions within the same server at the same time.
 */
public abstract class AbstractJobScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJobScheduler.class);
  private static final int SCHEDULER_ATTEMPTS = 5;

  private final Clock clock;
  private final Database database = DB.getDefault();
  private final DurableJobRegistry durableJobRegistry;
  private final PersistedDurableJobRepository persistedDurableJobRepository;

  public AbstractJobScheduler(
      Clock clock,
      DurableJobRegistry durableJobRegistry,
      PersistedDurableJobRepository persistedDurableJobRepository) {
    this.clock = Preconditions.checkNotNull(clock);
    this.durableJobRegistry = Preconditions.checkNotNull(durableJobRegistry);
    this.persistedDurableJobRepository = Preconditions.checkNotNull(persistedDurableJobRepository);
  }

  /** Returns the list of allowed {$JobType}s that can this scheduler can process */
  protected abstract ImmutableList<JobType> allowedJobTypes();

  /** Get an existing job from the database or an empty optional if one does not exist */
  protected abstract Optional<PersistedDurableJobModel> findScheduledJob(
      DurableJobRegistry.RegisteredJob registeredJob);

  /**
   * Checks that each recurring job in {@link DurableJobRegistry} has a {@link
   * PersistedDurableJobModel} scheduled sometime in the future and schedules one if not.
   *
   * <p>{@code synchronized} to avoid overlapping executions within the same server.
   */
  public synchronized void scheduleRecurringJobs() {
    for (DurableJobRegistry.RegisteredJob registeredJob : durableJobRegistry.getRecurringJobs()) {
      if (registeredJob.getRecurringJobExecutionTimeResolver().isEmpty()) {
        LOGGER.error(
            "JobScheduler_SchedulingError No RecurringJobExecutionTimeResolver registered with {}",
            registeredJob.getJobName());
        continue;
      }

      if (!allowedJobTypes().contains(registeredJob.getJobType())) {
        continue;
      }

      Instant executionTime =
          registeredJob.getRecurringJobExecutionTimeResolver().get().resolveExecutionTime(clock);

      boolean jobAlreadyScheduled = findScheduledJob(registeredJob).isPresent();

      if (!jobAlreadyScheduled) {
        PersistedDurableJobModel newJob =
            new PersistedDurableJobModel(
                registeredJob.getJobName().getJobNameString(),
                registeredJob.getJobType(),
                executionTime);
        try {
          tryScheduleRecurringJob(newJob, SCHEDULER_ATTEMPTS);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  protected synchronized void tryScheduleRecurringJob(
      PersistedDurableJobModel newJob, int remainingAttempts) throws InterruptedException {
    try (Transaction transaction = database.beginTransaction(TxIsolation.SERIALIZABLE)) {

      // Re-fetch upon each attempt so the transaction prevents duplicates.

      if (!persistedDurableJobRepository
          .findScheduledJob(newJob.getJobName(), newJob.getExecutionTime())
          .isPresent()) {
        newJob.save(transaction);
        transaction.commit();
      }
    } catch (OptimisticLockException e) {
      if (remainingAttempts > 0) {
        LOGGER.warn(
            "JobScheduler_SchedulingError OptimisticLockException scheduling {} with {} remaining"
                + " attempts",
            newJob.getJobName(),
            remainingAttempts);

        // Sleep for up to 100ms in effort to avoid transaction contention with other servers
        // that may be attempting to schedule the job at the same time.
        Thread.sleep(new Random().nextInt(/* bound= */ 100));
        tryScheduleRecurringJob(newJob, remainingAttempts--);
      } else {
        LOGGER.error(
            "JobScheduler_SchedulingError OptimisticLockException scheduling {}, no attempts"
                + " remaining!",
            newJob.getJobName());
      }
    }
  }
}
