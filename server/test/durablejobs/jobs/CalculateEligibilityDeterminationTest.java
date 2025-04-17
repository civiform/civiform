package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import durablejobs.DurableJobName;
import java.time.Instant;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationModel;
import models.EligibilityDetermination;
import models.JobType;
import models.PersistedDurableJobModel;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import repository.ResetPostgres;
import services.applicant.ApplicantService;
import services.applications.ApplicationService;

@RunWith(MockitoJUnitRunner.class)
public class CalculateEligibilityDeterminationTest extends ResetPostgres {

  ApplicantService applicantService = instanceOf(ApplicantService.class);
  ApplicationService applicationService = instanceOf(ApplicationService.class);
  ApplicationModel applicationModel = instanceOf(ApplicationModel.class);
  PersistedDurableJobModel jobModel =
      new PersistedDurableJobModel(
          DurableJobName.CALCULATE_ELIGIBILITY_DETERMINATION_JOB.toString(),
          JobType.RUN_ONCE,
          Instant.now());
  ApplicantModel applicant = new ApplicantModel();

  @Before
  public void setup() {
    String firstName = "firstName";
    String middleName = "middleName";
    String lastName = "lastName";
    String suffix = "";
    String emailAddress = "email@address.com";
    String countryCode = "US";
    String phoneNumber = "1234567890";
    ApplicantModel applicant = new ApplicantModel();
    applicant.setFirstName(firstName);
    applicant.setMiddleName(middleName);
    applicant.setLastName(lastName);
    applicant.setSuffix(suffix);
    applicant.setEmailAddress(emailAddress);
    applicant.setCountryCode(countryCode);
    applicant.setPhoneNumber(phoneNumber);
    AccountModel accountModel = new AccountModel();
    applicant.setAccount(accountModel);
    accountModel.setGlobalAdmin(false);
    applicant.save();
  }

  @Test
  public void run_calculateEligibilityDeterminationIfNoneIsNotComputed() {
    ProgramModel program = resourceCreator.insertActiveProgram("program");
    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    application.setEligibilityDetermination(EligibilityDetermination.NO_ELIGIBILITY_CRITERIA);

    CalculateEligibilityDeterminationJob job =
        new CalculateEligibilityDeterminationJob(applicantService, jobModel);
    job.run();
    application.refresh();

    assertThat(application.getEligibilityDetermination())
        .isEqualTo(EligibilityDetermination.NO_ELIGIBILITY_CRITERIA);
  }

  @Test
  public void run_calculateEligibilityDeterminationIfOneIsNotComputed() {
    ProgramModel program = resourceCreator.insertActiveProgram("program");
    ApplicationModel application =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);

    CalculateEligibilityDeterminationJob job =
        new CalculateEligibilityDeterminationJob(applicantService, jobModel);
    job.run();
    application.refresh();

    assertThat(application.getEligibilityDetermination())
        .isEqualTo(EligibilityDetermination.NO_ELIGIBILITY_CRITERIA);
  }

  @Test
  public void run_calculateEligibilityDeterminationIfMultipleAreNotComputed() {
    ProgramModel program = resourceCreator.insertActiveProgram("program");
    ApplicationModel firstApp =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    ApplicationModel secondApp =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    ApplicationModel thirdApp =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);

    CalculateEligibilityDeterminationJob job =
        new CalculateEligibilityDeterminationJob(applicantService, jobModel);
    job.run();
    firstApp.refresh();
    secondApp.refresh();
    thirdApp.refresh();

    assertThat(firstApp.getEligibilityDetermination())
        .isEqualTo(EligibilityDetermination.NO_ELIGIBILITY_CRITERIA);
    assertThat(secondApp.getEligibilityDetermination())
        .isEqualTo(EligibilityDetermination.NO_ELIGIBILITY_CRITERIA);
    assertThat(thirdApp.getEligibilityDetermination())
        .isEqualTo(EligibilityDetermination.NO_ELIGIBILITY_CRITERIA);
  }
}
