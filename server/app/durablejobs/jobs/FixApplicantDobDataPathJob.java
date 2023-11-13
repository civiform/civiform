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
 * Fixes applicant data objects that have the wrong path for applicant_date_of_birth and are causing
 * errors when TIs attempt to apply on behalf of clients. This job finds all applicant data objects
 * with applicant_date_of_birth set to a number and changes them to nested objects of the form
 * {applicant_date_of_birth: {date: number}}.
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
        "SELECT * FROM applicants WHERE ((object"
            + " #>>'{}')::jsonb)::json#>'{applicant,applicant_date_of_birth}' IS NOT NULL";
    database
        .findNative(Applicant.class, sql)
        .findEach(
            (applicant) -> {
              ApplicantData applicantData = applicant.getApplicantData();
              // Check that the applicant has a value for dob and that the path
              // is the deprecated path
              if (applicantData.getDateOfBirth().isPresent()
                  && !applicantData.hasPath(WellKnownPaths.APPLICANT_DOB)) {
                LocalDate dob = applicantData.getDeprecatedDateOfBirth().get();
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
