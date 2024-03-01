package durablejobs;

import models.PersistedDurableJobModel;

/**
 * Links an instance of a {@link DurableJob} with its concrete type.
 *
 * <p>Concrete implementations of {@link DurableJob} are linked to {@link PersistedDurableJobModel}
 * records in the database via their {@code DurableJobName}.
 */
public enum DurableJobName {
  OLD_JOB_CLEANUP("OLD_JOB_CLEANUP"),
  REPORTING_DASHBOARD_MONTHLY_REFRESH("REPORTING_DASHBOARD_MONTHLY_REFRESH"),
  UNUSED_ACCOUNT_CLEANUP("UNUSED_ACCOUNT_CLEANUP"),
  FIX_APPLICANT_DOB_DATA_PATH("FIX_APPLICANT_DOB_DATA_PATH"),

  UNUSED_PROGRAM_IMAGES_CLEANUP("UNUSED_PROGRAM_IMAGES_CLEANUP"),

  // job names used for tests
  TEST("TEST");

  private final String jobName;

  DurableJobName(String jobName) {
    this.jobName = jobName;
  }

  public String getJobNameString() {
    return jobName;
  }
}
