package durablejobs;

import static org.checkerframework.errorprone.com.google.common.base.Preconditions.checkNotNull;

import annotations.BindingAnnotations.StartupJobsProviderName;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import models.JobType;
import models.PersistedDurableJobModel;
import repository.PersistedDurableJobRepository;

/**
 * Schedules recurring jobs in the future.
 *
 * <p>RecurringJobScheduler is a singleton and its methods are {@code synchronized} to prevent
 * overlapping executions within the same server at the same time.
 */
@Singleton
public final class StartupJobScheduler extends AbstractJobScheduler {
  private final PersistedDurableJobRepository persistedDurableJobRepository;

  @Inject
  public StartupJobScheduler(
      Clock clock,
      @StartupJobsProviderName DurableJobRegistry durableJobRegistry,
      PersistedDurableJobRepository persistedDurableJobRepository) {
    super(clock, durableJobRegistry, persistedDurableJobRepository);
    this.persistedDurableJobRepository = checkNotNull(persistedDurableJobRepository);
  }

  /** Returns the list of allowed {$JobType}s that can this scheduler can process */
  @Override
  protected synchronized ImmutableList<JobType> allowedJobTypes() {
    return ImmutableList.<JobType>builder()
        .add(JobType.RUN_ON_EACH_STARTUP)
        .add(JobType.RUN_ONCE)
        .build();
  }

  /** Get an existing job from the database or an empty optional if one does not exist */
  @Override
  protected synchronized Optional<PersistedDurableJobModel> findScheduledJob(
      DurableJobRegistry.RegisteredJob registeredJob) {
    var scheduledJob =
        persistedDurableJobRepository.findScheduledJob(
            registeredJob.getJobName().getJobNameString());

    // JobType.RUN_ONCE must never run again
    if (scheduledJob.isPresent() && scheduledJob.get().getJobType() == JobType.RUN_ONCE) {
      return scheduledJob;
    }

    return Optional.empty();
  }
}
