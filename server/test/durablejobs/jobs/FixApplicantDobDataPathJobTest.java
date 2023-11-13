package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import durablejobs.DurableJobName;
import io.ebean.DB;
import io.ebean.Database;
import java.time.Instant;
import java.util.List;
import models.Applicant;
import models.PersistedDurableJob;
import org.junit.Test;
import repository.ResetPostgres;
import services.WellKnownPaths;

public class FixApplicantDobDataPathJobTest extends ResetPostgres {

  @Test
  public void run_correctlyFixesDobPaths() {
    Applicant applicantWithCorrectPath = new Applicant();
    applicantWithCorrectPath.getApplicantData().setUserName("Foo");
    applicantWithCorrectPath.getApplicantData().setDateOfBirth("2001-11-01");
    applicantWithCorrectPath.save();

    Applicant applicantWithDeprecatedPath = new Applicant();
    applicantWithDeprecatedPath.getApplicantData().setUserName("Bar");
    applicantWithDeprecatedPath
        .getApplicantData()
        .putDate(WellKnownPaths.APPLICANT_DOB_DEPRECATED, "2002-12-02");
    applicantWithDeprecatedPath.save();

    PersistedDurableJob job =
        new PersistedDurableJob(
            DurableJobName.FIX_APPLICANT_DOB_DATA_PATH.toString(), Instant.ofEpochMilli(1000));
    FixApplicantDobDataPathJob fixApplicantDobDataPathJob = new FixApplicantDobDataPathJob(job);

    fixApplicantDobDataPathJob.run();

    Database database = DB.getDefault();
    List<Applicant> applicants = database.find(Applicant.class).findList();

    // The applicant with the correct dob path should not have been updated
    assertThat(applicants.get(0).getApplicantData().asJsonString())
        .isEqualTo(applicantWithCorrectPath.getApplicantData().asJsonString());

    // The applicant with the deprecated dob path should now have the correct nested path
    String updatedObject =
        "{\"applicant\":{\"name\":{\"first_name\":\"Bar\"},\"applicant_date_of_birth\":{\"date\":1038787200000}}}";
    assertThat(applicants.get(1).getApplicantData().asJsonString()).isEqualTo(updatedObject);
  }
}
