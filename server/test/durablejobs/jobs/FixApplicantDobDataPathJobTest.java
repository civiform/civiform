package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import durablejobs.DurableJobName;
import java.time.Instant;
import models.ApplicantModel;
import models.PersistedDurableJobModel;
import org.junit.Test;
import repository.AccountRepository;
import repository.ResetPostgres;
import services.WellKnownPaths;
import services.applicant.ApplicantData;

public class FixApplicantDobDataPathJobTest extends ResetPostgres {

  @Test
  public void run_correctlyFixesDobPaths() {
    // Add applicant with the correct dob path
    ApplicantModel applicantWithCorrectPath = new ApplicantModel();
    ApplicantData applicantDataWithCorrectPath = applicantWithCorrectPath.getApplicantData();
    applicantDataWithCorrectPath.setUserName("Foo");
    applicantDataWithCorrectPath.setDateOfBirth("2001-11-01");
    applicantWithCorrectPath.save();
    String applicantDataWithCorrectPathJson = applicantDataWithCorrectPath.asJsonString();

    // Add applicant with the incorrect dob path
    ApplicantModel applicantWithDeprecatedPath = new ApplicantModel();
    ApplicantData applicantDataWithDeprecatedPath = applicantWithDeprecatedPath.getApplicantData();
    applicantDataWithDeprecatedPath.setUserName("Bar");
    applicantDataWithDeprecatedPath.putDate(WellKnownPaths.APPLICANT_DOB_DEPRECATED, "2002-12-02");
    applicantWithDeprecatedPath.save();

    // Run the job
    AccountRepository accountRepository = instanceOf(AccountRepository.class);
    PersistedDurableJobModel job =
        new PersistedDurableJobModel(
            DurableJobName.FIX_APPLICANT_DOB_DATA_PATH.toString(), Instant.ofEpochMilli(1000));
    FixApplicantDobDataPathJob fixApplicantDobDataPathJob =
        new FixApplicantDobDataPathJob(accountRepository, job);
    fixApplicantDobDataPathJob.run();

    applicantWithCorrectPath.refresh();
    applicantWithDeprecatedPath.refresh();

    // The applicant with the correct dob path should not have been updated
    assertThat(applicantWithCorrectPath.getApplicantData().asJsonString())
        .isEqualTo(applicantDataWithCorrectPathJson);
    // The applicant with the deprecated dob path should now have the correct nested path
    String updatedObject =
        "{\"applicant\":{\"name\":{\"first_name\":\"Bar\"},\"applicant_date_of_birth\":{\"date\":1038787200000}}}";
    assertThat(applicantWithDeprecatedPath.getApplicantData().asJsonString())
        .isEqualTo(updatedObject);
  }
}
