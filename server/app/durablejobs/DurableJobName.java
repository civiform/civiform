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
  UNUSED_PROGRAM_IMAGES_CLEANUP("UNUSED_PROGRAM_IMAGES_CLEANUP"),
  MIGRATE_PRIMARY_APPLICANT_INFO("MIGRATE_PRIMARY_APPLICANT_INFO"),
  ADD_OPERATOR_TO_LEAF_ADDRESS_SERVICE_AREA("ADD_OPERATOR_TO_LEAF_ADDRESS_SERVICE_AREA"),

  // Jobs below this line are deprecated, but must be kept around so that durableJobRegistry.get
  // does not throw an IllegalArgumentException error
  // TODO(#7347): remove the deprecated job names once we have logic in place to remove them from
  // the db when not found
  FIX_APPLICANT_DOB_DATA_PATH("FIX_APPLICANT_DOB_DATA_PATH"),

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
