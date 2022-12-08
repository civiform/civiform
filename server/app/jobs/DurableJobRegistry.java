package jobs;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Optional;

public final class DurableJobRegistry {

  private final HashMap<String, RegisteredJob> registeredJobs = new HashMap<>();

  @AutoValue
  public abstract static class RegisteredJob {

    public static RegisteredJob create(
        DurableJobFactory durableJobFactory,
        DurableJobName jobName,
        Optional<RecurringJobExecutionTimeResolver> recurringJobExecutionTimeResolver) {
      return new AutoValue_DurableJobRegistry_RegisteredJob(
          durableJobFactory, jobName, recurringJobExecutionTimeResolver);
    }

    public abstract DurableJobFactory getFactory();

    public abstract DurableJobName getJobName();

    public abstract Optional<RecurringJobExecutionTimeResolver>
        getRecurringJobExecutionTimeResolver();

    public boolean isRecurring() {
      return this.getRecurringJobExecutionTimeResolver().isPresent();
    }
  }

  /** Register a factory for a given job name. */
  public void register(DurableJobName jobName, DurableJobFactory durableJobFactory) {
    registeredJobs.put(
        jobName.getJobName(),
        RegisteredJob.create(
            durableJobFactory, jobName, /* recurringJobExecutionTimeResolver */ Optional.empty()));
  }

  /**
   * Register a factory for a given job name along with a {@link RecurringJobExecutionTimeResolver}
   * to make it a recurring job.
   */
  public void register(
      DurableJobName jobName,
      DurableJobFactory durableJobFactory,
      RecurringJobExecutionTimeResolver recurringJobExecutionTimeResolver) {
    registeredJobs.put(
        jobName.getJobName(),
        RegisteredJob.create(
            durableJobFactory, jobName, Optional.of(recurringJobExecutionTimeResolver)));
  }

  /** Retrieves the job registered with the given name or throws {@link JobNotFoundException}. */
  public RegisteredJob get(DurableJobName jobName) throws JobNotFoundException {
    return Optional.ofNullable(registeredJobs.get(jobName.getJobName()))
        .orElseThrow(() -> new JobNotFoundException(jobName.getJobName()));
  }

  /** Get all jobs that were registered with a {@link RecurringJobExecutionTimeResolver}. */
  public ImmutableSet<RegisteredJob> getRecurringJobs() {
    return registeredJobs.values().stream()
        .filter(RegisteredJob::isRecurring)
        .collect(ImmutableSet.toImmutableSet());
  }
}
