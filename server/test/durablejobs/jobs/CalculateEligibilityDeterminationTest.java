package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static support.FakeRequestBuilder.fakeRequest;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableMap;
import durablejobs.DurableJobName;
import io.ebean.DB;
import java.time.Instant;
import java.util.Locale;
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
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.question.Scalar;
import services.applications.ApplicationService;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import support.TestProgramUtility;

@RunWith(MockitoJUnitRunner.class)
public class CalculateEligibilityDeterminationTest extends ResetPostgres {

  ApplicantService applicantService;
  private VersionRepository versionRepository;
  private QuestionService questionService;
  ApplicationService applicationService = instanceOf(ApplicationService.class);
  ProgramService programService = instanceOf(ProgramService.class);
  ApplicationModel applicationModel = instanceOf(ApplicationModel.class);
  CiviFormProfile trustedIntermediaryProfile;
  ProgramDefinition programDefinition;
  NameQuestionDefinition questionDefinition;
  PersistedDurableJobModel jobModel =
      new PersistedDurableJobModel(
          DurableJobName.CALCULATE_ELIGIBILITY_DETERMINATION_JOB.toString(),
          JobType.RUN_ONCE,
          Instant.now());

  @Before
  public void setup() {
    // Initialize applicantService as a spy so we can mock specific method calls
    applicantService = spy(instanceOf(ApplicantService.class));
    versionRepository = instanceOf(VersionRepository.class);
    questionService = instanceOf(QuestionService.class);
    trustedIntermediaryProfile = Mockito.mock(CiviFormProfile.class);
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
        new CalculateEligibilityDeterminationJob(applicantService, programService, jobModel);
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
        new CalculateEligibilityDeterminationJob(applicantService, programService, jobModel);
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
        new CalculateEligibilityDeterminationJob(applicantService, programService, jobModel);
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
  public void run_setsEligibilityDeterminationToIneligible() throws Exception {
    NameQuestionDefinition questionDefinition =
        (NameQuestionDefinition)
            questionService
                .create(
                    new NameQuestionDefinition(
                        QuestionDefinitionConfig.builder()
                            .setName("name question")
                            .setDescription("description")
                            .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
                            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
                            .build()))
                .getResult();
    programDefinition =
        TestProgramUtility.createProgramWithNongatingEligibility(
            questionService, versionRepository, questionDefinition);
    ApplicantModel applicant = applicantService.createApplicant().toCompletableFuture().join();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();

    Path questionPath =
        ApplicantData.APPLICANT_PATH.join(questionDefinition.getQuestionPathSegment());
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(questionPath.join(Scalar.FIRST_NAME).toString(), "Ineligible answer")
            .put(questionPath.join(Scalar.LAST_NAME).toString(), "irrelevant answer")
            .build();
    applicantService
        .stageAndUpdateIfValid(
            applicant.id,
            programDefinition.id(),
            "1",
            updates,
            false,
            false,
            /* apiBridgeEnabled= */ false)
        .toCompletableFuture()
        .join();
    ApplicationModel application =
        applicantService
            .submitApplication(
                applicant.id, programDefinition.id(), trustedIntermediaryProfile, fakeRequest())
            .toCompletableFuture()
            .join();

    assertThat(application.getEligibilityDetermination())
        .isEqualTo(EligibilityDetermination.INELIGIBLE);

    // Set EligibilityDetermination to NOT_COMPUTED to mock the existing applications with
    // eligibility_determination default value
    application.setEligibilityDetermination(EligibilityDetermination.NOT_COMPUTED);
    assertThat(application.getEligibilityDetermination())
        .isEqualTo(EligibilityDetermination.NOT_COMPUTED);

    CalculateEligibilityDeterminationJob job =
        new CalculateEligibilityDeterminationJob(applicantService, programService, jobModel);
    job.run();
    application.refresh();

    assertThat(application.getEligibilityDetermination())
        .isEqualTo(EligibilityDetermination.INELIGIBLE);
    assertThat(jobModel.getErrorMessage()).isEmpty();
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
        new CalculateEligibilityDeterminationJob(applicantService, programService, jobModel);
    job.run();

    applicationsToProcess.forEach(ApplicationModel::refresh);

    long notComputedCount =
        applicationsToProcess.stream()
            .filter(
                app ->
                    app.getEligibilityDetermination().equals(EligibilityDetermination.NOT_COMPUTED))
            .count();
    assertThat(notComputedCount).isEqualTo(11);
    assertThat(jobModel.getErrorMessage())
        .contains("Eligibility Determination: stopping early after 10 errors");
  }
}
