package durablejobs;

import com.google.common.base.Preconditions;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import java.time.Clock;
import java.time.Instant;
import java.util.Random;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.OptimisticLockException;
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
@Singleton
public final class RecurringJobScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RecurringJobScheduler.class);
  private static final int SCHEDULER_ATTEMPTS = 5;

  private final Clock clock;
  private final Database database = DB.getDefault();
  private final DurableJobRegistry durableJobRegistry;
  private final PersistedDurableJobRepository persistedDurableJobRepository;

  @Inject
  public RecurringJobScheduler(
      Clock clock,
      DurableJobRegistry durableJobRegistry,
      PersistedDurableJobRepository persistedDurableJobRepository) {
    this.clock = Preconditions.checkNotNull(clock);
    this.durableJobRegistry = Preconditions.checkNotNull(durableJobRegistry);
    this.persistedDurableJobRepository = Preconditions.checkNotNull(persistedDurableJobRepository);
  }

  /**
   * Checks that each recurring job in {@link DurableJobRegistry} has a {@link
   * PersistedDurableJobModel} scheduled sometime in the future and schedules one if not.
   *
   * <p>{@code synchronized} to avoid overlapping executions within the same server.
   */
  public synchronized void scheduleRecurringJobs() {
    for (DurableJobRegistry.RegisteredJob registeredJob : durableJobRegistry.getRecurringJobs()) {
      if (registeredJob.getExecutionTimeResolver().isEmpty()) {
        LOGGER.error(
            "JobScheduler_SchedulingError No ExecutionTimeResolver registered with {}",
            registeredJob.getJobName());
        continue;
      }

      Instant executionTime =
          registeredJob.getExecutionTimeResolver().get().resolveExecutionTime(clock);
      boolean jobAlreadyScheduled =
          persistedDurableJobRepository
              .findScheduledJob(registeredJob.getJobName().getJobNameString(), executionTime)
              .isPresent();

      if (!jobAlreadyScheduled) {
        PersistedDurableJobModel newJob =
            new PersistedDurableJobModel(
                registeredJob.getJobName().getJobNameString(), executionTime);
        try {
          tryScheduleRecurringJob(newJob, SCHEDULER_ATTEMPTS);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  private synchronized void tryScheduleRecurringJob(
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
