package durablejobs;

import java.util.Optional;
import models.PersistedDurableJobModel;

/**
 * Links an instance of a {@link DurableJob} with its concrete type.
 *
 * <p>Concrete implementations of {@link DurableJob} are linked to {@link PersistedDurableJobModel}
 * records in the database via their {@code DurableJobName}.
 *
 * <p>See <a href="https://github.com/civiform/civiform/wiki/Durable-Jobs#naming-new-jobs">wiki</a>
 * for details on job naming conventions.
 */
public enum DurableJobName {
  OLD_JOB_CLEANUP("OLD_JOB_CLEANUP"),
  REPORTING_DASHBOARD_MONTHLY_REFRESH("REPORTING_DASHBOARD_MONTHLY_REFRESH"),
  UNUSED_ACCOUNT_CLEANUP("UNUSED_ACCOUNT_CLEANUP"),
  UNUSED_PROGRAM_IMAGES_CLEANUP("UNUSED_PROGRAM_IMAGES_CLEANUP"),
  ADD_OPERATOR_TO_LEAF_ADDRESS_SERVICE_AREA("ADD_OPERATOR_TO_LEAF_ADDRESS_SERVICE_AREA"),
  COPY_FILE_KEY_FOR_MULTIPLE_FILE_UPLOAD("COPY_FILE_KEY_FOR_MULTIPLE_FILE_UPLOAD"),
  CONVERT_ADDRESS_SERVICE_AREA_TO_ARRAY("CONVERT_ADDRESS_SERVICE_AREA_TO_ARRAY"),
  ADD_CATEGORY_AND_TRANSLATION("ADD_CATEGORY_AND_TRANSLATION"),

  // job names used for tests
  TEST("TEST");

  private final String jobName;

  DurableJobName(String jobName) {
    this.jobName = jobName;
  }

  public String getJobNameString() {
    return jobName;
  }

  public static Optional<DurableJobName> optionalValueOf(String jobName) {
    try {
      return Optional.of(DurableJobName.valueOf(jobName));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
