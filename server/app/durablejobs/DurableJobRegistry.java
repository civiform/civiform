package durablejobs;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Optional;
import models.JobType;
import models.PersistedDurableJobModel;

/**
 * Provides the means of looking up a {@link DurableJob} by its {@link DurableJobName}. This is
 * necessary because all {@link DurableJob}s are persisted by {@link PersistedDurableJobModel}
 * records. Also provides the means of retrieving a list of all recurring jobs with their associated
 * {@link JobExecutionTimeResolver}.
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
   * optionally a {@link JobExecutionTimeResolver} if they are recurring jobs.
   */
  @AutoValue
  public abstract static class RegisteredJob {

    private static RegisteredJob create(
        DurableJobFactory durableJobFactory,
        DurableJobName jobName,
        JobType jobType,
        Optional<JobExecutionTimeResolver> jobExecutionTimeResolver) {
      return new AutoValue_DurableJobRegistry_RegisteredJob(
          durableJobFactory, jobName, jobType, jobExecutionTimeResolver);
    }

    /** The {@link DurableJobFactory} factory for this {@link DurableJob}. */
    public abstract DurableJobFactory getFactory();

    /** The {@link DurableJobName} for this {@link DurableJob}. */
    public abstract DurableJobName getJobName();

    public abstract JobType getJobType();

    /**
     * The {@link JobExecutionTimeResolver} for this {@link DurableJob}.
     *
     * <p>If not present then this job is not recurring.
     */
    public abstract Optional<JobExecutionTimeResolver> getRecurringJobExecutionTimeResolver();

    /** True if this job is recurring. */
    public boolean isRecurring() {
      return this.getRecurringJobExecutionTimeResolver().isPresent();
    }
  }

  /** Registers a factory for a given job name. */
  @VisibleForTesting
  public void registerWithNoTimeResolver(
      DurableJobName jobName, JobType jobType, DurableJobFactory durableJobFactory) {
    validateJobName(jobName);

    registeredJobs.put(
        jobName.getJobNameString(),
        RegisteredJob.create(
            durableJobFactory, jobName, jobType, /* jobExecutionTimeResolver= */ Optional.empty()));
  }

  /** Registers a factory for a given job name. Cannot be used with {$JobType.RECURRING}. */
  public void registerStartupJob(
      DurableJobName jobName, JobType jobType, DurableJobFactory durableJobFactory) {
    validateJobName(jobName);
    if (jobType == JobType.RECURRING) {
      throw new IllegalArgumentException("Startup jobs cannot have a JobType.RECURRING.");
    }

    // Resolver to provide an immediate time for non-recurring jobs. This is here to keep it
    // contained and not accidentally used by recurring jobs as it will cause problems because of
    // job the pekko scheduler repeats.
    class ImmediateJobExecutionTimeResolver implements JobExecutionTimeResolver {
      @Override
      public Instant resolveExecutionTime(Clock clock) {
        return LocalDateTime.now(clock).minusSeconds(30).toInstant(ZoneOffset.UTC);
      }
    }

    registeredJobs.put(
        jobName.getJobNameString(),
        RegisteredJob.create(
            durableJobFactory,
            jobName,
            jobType,
            Optional.of(new ImmediateJobExecutionTimeResolver())));
  }

  /**
   * Registers a factory for a given job name along with a {@link JobExecutionTimeResolver} that
   * defines the future run times of the job. Can only be used with {$JobType.RECURRING}.
   */
  public void register(
      DurableJobName jobName,
      JobType jobType,
      DurableJobFactory durableJobFactory,
      JobExecutionTimeResolver jobExecutionTimeResolver) {
    validateJobName(jobName);
    if (jobType != JobType.RECURRING) {
      throw new IllegalArgumentException("Recurring jobs must have JobType.RECURRING.");
    }

    registeredJobs.put(
        jobName.getJobNameString(),
        RegisteredJob.create(
            durableJobFactory, jobName, jobType, Optional.of(jobExecutionTimeResolver)));
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

  /** Returns all jobs registered with a {@link JobExecutionTimeResolver}. */
  public ImmutableSet<RegisteredJob> getRecurringJobs() {
    return registeredJobs.values().stream()
        .filter(RegisteredJob::isRecurring)
        .collect(ImmutableSet.toImmutableSet());
  }
}
