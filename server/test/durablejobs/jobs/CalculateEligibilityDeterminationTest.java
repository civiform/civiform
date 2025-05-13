package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import durablejobs.DurableJobName;
import io.ebean.DB;
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
import services.applicant.ReadOnlyApplicantProgramService;
import services.applications.ApplicationService;
import services.program.ProgramDefinition;

@RunWith(MockitoJUnitRunner.class)
public class CalculateEligibilityDeterminationTest extends ResetPostgres {

  ApplicantService applicantService;
  ApplicationService applicationService = instanceOf(ApplicationService.class);
  ApplicationModel applicationModel = instanceOf(ApplicationModel.class);
  PersistedDurableJobModel jobModel =
      new PersistedDurableJobModel(
          DurableJobName.CALCULATE_ELIGIBILITY_DETERMINATION_JOB.toString(),
          JobType.RUN_ONCE,
          Instant.now());

  @Before
  public void setup() {
    // Initialize applicantService as a spy so we can mock specific method calls
    applicantService = spy(instanceOf(ApplicantService.class));

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
    assertThat(application.getEligibilityDetermination())
        .isEqualTo(EligibilityDetermination.NOT_COMPUTED);

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
    assertThat(firstApp.getEligibilityDetermination())
        .isEqualTo(EligibilityDetermination.NOT_COMPUTED);
    assertThat(secondApp.getEligibilityDetermination())
        .isEqualTo(EligibilityDetermination.NOT_COMPUTED);
    assertThat(thirdApp.getEligibilityDetermination())
        .isEqualTo(EligibilityDetermination.NOT_COMPUTED);

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

  @Test
  public void run_stopsEarlyWhenErrorsExceedLimit() throws Exception {
    ProgramModel program = resourceCreator.insertActiveProgram("program");
    for (int i = 0; i < 11; i++) {
      resourceCreator.insertActiveApplication(
          resourceCreator.insertApplicantWithAccount(), program);
    }
    var applicationsToProcess =
        DB.getDefault()
            .find(ApplicationModel.class)
            .where()
            .eq("eligibility_determination", EligibilityDetermination.NOT_COMPUTED)
            .eq("lifecycle_stage", "active")
            .findList();

    assertThat(applicationsToProcess).hasSize(11);
    doThrow(new RuntimeException("Simulated eligibility calculation error"))
        .when(applicantService)
        .calculateEligibilityDetermination(
            any(ProgramDefinition.class), any(ReadOnlyApplicantProgramService.class));

    CalculateEligibilityDeterminationJob job =
        new CalculateEligibilityDeterminationJob(applicantService, jobModel);
    job.run();

    // Refresh all applications to check their final state
    applicationsToProcess.forEach(ApplicationModel::refresh);

    long notComputedCount =
        applicationsToProcess.stream()
            .filter(
                app ->
                    app.getEligibilityDetermination().equals(EligibilityDetermination.NOT_COMPUTED))
            .count();
    assertThat(notComputedCount).isGreaterThanOrEqualTo(10);
    assertThat(jobModel.getErrorMessage()).contains("Stopping early after 10 errors");
  }
}
