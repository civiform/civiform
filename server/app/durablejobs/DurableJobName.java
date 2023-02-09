package durablejobs;

/**
 * Links an instance of a {@link DurableJob} with its concrete type.
 *
 * <p>Concrete implementations of {@link DurableJob} are linked to {@link
 * models.PersistedDurableJob} records in the database via their {@code DurableJobName}.
 */
public enum DurableJobName {
  OLD_JOB_CLEANUP("OLD_JOB_CLEANUP");

  private final String jobName;

  DurableJobName(String jobName) {
    this.jobName = jobName;
  }

  public String getJobName() {
    return jobName;
  }
}
