package durablejobs;

import static org.checkerframework.errorprone.com.google.common.base.Preconditions.checkNotNull;

import annotations.BindingAnnotations.RecurringJobsProviderName;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import models.JobType;
import models.PersistedDurableJobModel;
import repository.PersistedDurableJobRepository;

/**
 * Schedules recurring jobs in the future.
 *
 * <p>{@link RecurringJobScheduler} is a singleton and its methods are {@code synchronized} to
 * prevent overlapping executions within the same server at the same time.
 */
@Singleton
public final class RecurringJobScheduler extends AbstractJobScheduler {
  private final Clock clock;
  private final PersistedDurableJobRepository persistedDurableJobRepository;

  @Inject
  public RecurringJobScheduler(
      Clock clock,
      @RecurringJobsProviderName DurableJobRegistry durableJobRegistry,
      PersistedDurableJobRepository persistedDurableJobRepository) {
    super(clock, durableJobRegistry, persistedDurableJobRepository);
    this.clock = checkNotNull(clock);
    this.persistedDurableJobRepository = checkNotNull(persistedDurableJobRepository);
  }

  /** Returns the list of allowed {@link JobType}s that can this scheduler can process */
  @Override
  protected synchronized ImmutableList<JobType> allowedJobTypes() {
    // If more recurring options are added in the future the DurableJobRegistry#registerStartupJob
    // needs to be updated to handle it.
    return ImmutableList.<JobType>builder().add(JobType.RECURRING).build();
  }

  /** Get an existing job from the database or an empty optional if one does not exist */
  @Override
  protected synchronized Optional<PersistedDurableJobModel> findScheduledJob(
      DurableJobRegistry.RegisteredJob registeredJob) {
    Instant executionTime =
        registeredJob.getRecurringJobExecutionTimeResolver().get().resolveExecutionTime(clock);

    return persistedDurableJobRepository.findScheduledJob(
        registeredJob.getJobName().getJobNameString(), executionTime);
  }
}
