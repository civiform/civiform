package durablejobs;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Optional;
import models.PersistedDurableJobModel;

/**
 * Provides the means of looking up a {@link DurableJob} by its {@link DurableJobName}. This is
 * necessary because all {@link DurableJob}s are persisted by {@link PersistedDurableJobModel}
 * records. Also provides the means of retrieving a list of all recurring jobs with their associated
 * {@link ExecutionTimeResolver}.
 */
public final class DurableJobRegistry {

  private final HashMap<String, RegisteredJob> registeredJobs = new HashMap<>();

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("DurableJobRegistry");
    builder.append(registeredJobs.entrySet());
    return builder.toString();
  }

  /**
   * A {@link DurableJob} that has been registered with the {@link DurableJobRegistry}.
   *
   * <p>When added to the registry, jobs are associated with their factories, job names, and
   * optionally a {@link ExecutionTimeResolver} if they are recurring jobs.
   */
  @AutoValue
  public abstract static class RegisteredJob {

    private static RegisteredJob create(
        DurableJobFactory durableJobFactory,
        DurableJobName jobName,
        Optional<ExecutionTimeResolver> recurringJobExecutionTimeResolver) {
      return new AutoValue_DurableJobRegistry_RegisteredJob(
          durableJobFactory, jobName, recurringJobExecutionTimeResolver);
    }

    /** The {@link DurableJobFactory} factory for this {@link DurableJob}. */
    public abstract DurableJobFactory getFactory();

    /** The {@link DurableJobName} for this {@link DurableJob}. */
    public abstract DurableJobName getJobName();

    /**
     * The {@link ExecutionTimeResolver} for this {@link DurableJob}.
     *
     * <p>If not present then this job is not recurring.
     */
    public abstract Optional<ExecutionTimeResolver> getExecutionTimeResolver();

    /** True if this job is recurring. */
    public boolean isRecurring() {
      return this.getExecutionTimeResolver().isPresent();
    }
  }

  /** Registers a factory for a given job name. */
  public void register(DurableJobName jobName, DurableJobFactory durableJobFactory) {
    validateJobName(jobName);

    registeredJobs.put(
        jobName.getJobNameString(),
        RegisteredJob.create(
            durableJobFactory, jobName, /* recurringJobExecutionTimeResolver= */ Optional.empty()));
  }

  /**
   * Registers a factory for a given job name along with a {@link ExecutionTimeResolver} that
   * defines the future run times of the job.
   */
  public void register(
      DurableJobName jobName,
      DurableJobFactory durableJobFactory,
      ExecutionTimeResolver recurringJobExecutionTimeResolver) {
    validateJobName(jobName);

    registeredJobs.put(
        jobName.getJobNameString(),
        RegisteredJob.create(
            durableJobFactory, jobName, Optional.of(recurringJobExecutionTimeResolver)));
  }

  private void validateJobName(DurableJobName jobName) {
    if (registeredJobs.containsKey(jobName.getJobNameString())) {
      throw new IllegalArgumentException(
          String.format("Unable to register durable job with duplicate job name: %s", jobName));
    }
  }

  /** Retrieves the job registered with the given name or throws {@link JobNotFoundException}. */
  public RegisteredJob get(DurableJobName jobName) throws JobNotFoundException {
    return Optional.ofNullable(registeredJobs.get(jobName.getJobNameString()))
        .orElseThrow(() -> new JobNotFoundException(jobName.getJobNameString()));
  }

  /** Returns all jobs registered with a {@link ExecutionTimeResolver}. */
  public ImmutableSet<RegisteredJob> getRecurringJobs() {
    return registeredJobs.values().stream()
        .filter(RegisteredJob::isRecurring)
        .collect(ImmutableSet.toImmutableSet());
  }
}
