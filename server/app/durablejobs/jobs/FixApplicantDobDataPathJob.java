package durablejobs.jobs;

import com.google.common.base.Preconditions;
import durablejobs.DurableJob;
import io.ebean.DB;
import io.ebean.Database;
import java.time.LocalDate;
import models.Applicant;
import models.PersistedDurableJob;
import services.WellKnownPaths;
import services.applicant.ApplicantData;

/**
 * Finds applicant data objects with applicant_date_of_birth set to a number and changes it to a
 * nest object.
 */
public final class FixApplicantDobDataPathJob extends DurableJob {
  private final Database database;

  private final PersistedDurableJob persistedDurableJob;

  public FixApplicantDobDataPathJob(PersistedDurableJob persistedDurableJob) {
    this.persistedDurableJob = Preconditions.checkNotNull(persistedDurableJob);
    this.database = DB.getDefault();
  }

  @Override
  public PersistedDurableJob getPersistedDurableJob() {
    return persistedDurableJob;
  }

  @Override
  public void run() {
    String sql =
        "select * from applicants where ((object #>>"
            + " '{}')::jsonb)::json#>'{applicant,applicant_date_of_birth}' is not null";
    database
        .findNative(Applicant.class, sql)
        .findEach(
            (applicant) -> {
              ApplicantData applicantData = applicant.getApplicantData();
              // Check that the applicant has a value for dob and that the path
              // is the deprecated path
              if (applicantData.getDateOfBirth().isPresent()
                  && !applicantData.hasPath(WellKnownPaths.APPLICANT_DOB)) {
                LocalDate dob = applicantData.getDateOfBirth().get();
                // Clear the old path off the json data before inserting the new one
                applicantData
                    .getDocumentContext()
                    .delete(WellKnownPaths.APPLICANT_DOB_DEPRECATED.toString());
                applicantData.setDateOfBirth(dob.toString());
                applicant.save();
              }
            });
  }
}
