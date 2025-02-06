package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.FOUND;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import controllers.FlashKey;
import controllers.WithMockedProfiles;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationModel;
import models.LifecycleStage;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.ApplicationRepository;
import repository.VersionRepository;
import services.Path;
import services.applicant.question.Scalar;
import services.program.ProgramDefinition;
import support.ProgramBuilder;

public class ApplicantProgramReviewControllerTest extends WithMockedProfiles {

  private ApplicantProgramReviewController subject;
  private ApplicantProgramBlocksController blockController;
  private ProgramModel activeProgram;
  public ApplicantModel applicant;

  @Before
  public void setUpWithFreshApplicants() {
    resetDatabase();

    subject = instanceOf(ApplicantProgramReviewController.class);
    blockController = instanceOf(ApplicantProgramBlocksController.class);
    activeProgram =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    applicant = createApplicantWithMockedProfile();
  }

  @Test
  public void review_invalidApplicant_redirectsToHome() {
    long badApplicantId = applicant.id + 1000;
    Result result = this.review(badApplicantId, activeProgram.id);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void review_applicantAccessToDraftProgram_redirectsToHome() {
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    Result result = this.review(applicant.id, draftProgram.id);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void review_civiformAdminAccessToDraftProgram_isOk() {
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.newestApplicant().orElseThrow();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    Result result = this.review(applicant.id, draftProgram.id);
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void review_obsoleteProgram_isOk() {
    ProgramModel obsoleteProgram = ProgramBuilder.newObsoleteProgram("program").build();
    Result result = this.review(applicant.id, obsoleteProgram.id);
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void review_toAProgramThatDoesNotExist_returns404() {
    long badProgramId = activeProgram.id + 1000;
    Result result = this.review(applicant.id, badProgramId);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void review_rendersSummaryView() {
    Result result = this.review(applicant.id, activeProgram.id);
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void submit_invalid_redirectsToHome() {
    long badApplicantId = applicant.id + 1000;
    Result result = this.submit(badApplicantId, activeProgram.id);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void submit_applicantAccessToDraftProgram_redirectsToHome() {
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    Result result = this.submit(applicant.id, draftProgram.id);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void submit_civiformAdminAccessToDraftProgram_redirectsAndDoesNotSubmitApplication() {
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.newestApplicant().orElseThrow();

    ProgramBuilder.newActiveProgram("test program", "desc")
        .withBlock()
        .withRequiredQuestion(testQuestionBank().nameApplicantName())
        .buildDefinition();
    ProgramDefinition draftProgramDefinition =
        ProgramBuilder.newDraftProgram("test program")
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .buildDefinition();
    answer(draftProgramDefinition.id());

    Result result = this.submit(applicant.id, draftProgramDefinition.id());
    assertThat(result.status()).isEqualTo(SEE_OTHER);

    // No application was submitted
    ApplicationRepository applicationRepository = instanceOf(ApplicationRepository.class);
    ImmutableSet<ApplicationModel> applications =
        applicationRepository
            .getApplicationsForApplicant(applicant.id, ImmutableSet.of(LifecycleStage.ACTIVE))
            .toCompletableFuture()
            .join();
    assertThat(applications).hasSize(0);
    applications =
        applicationRepository
            .getApplicationsForApplicant(applicant.id, ImmutableSet.of(LifecycleStage.DRAFT))
            .toCompletableFuture()
            .join();
    assertThat(applications).hasSize(1);
    assertThat(applications.asList().get(0).getProgram().id).isEqualTo(draftProgramDefinition.id());
  }

  @Test
  public void submit_obsoleteProgram_isSuccessful_whenFastForwardDisabled() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc")
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .buildDefinition();
    resourceCreator().insertDraftProgram(programDefinition.adminName());
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    versionRepository.publishNewSynchronizedVersion();

    answer(programDefinition.id());

    Result result = this.submit(applicant.id, programDefinition.id());
    assertThat(result.status()).isEqualTo(FOUND);

    // An application was submitted
    ApplicationRepository applicationRepository = instanceOf(ApplicationRepository.class);
    ImmutableSet<ApplicationModel> applications =
        applicationRepository
            .getApplicationsForApplicant(applicant.id, ImmutableSet.of(LifecycleStage.ACTIVE))
            .toCompletableFuture()
            .join();
    assertThat(applications).hasSize(1);
    assertThat(applications.asList().get(0).getProgram().id).isEqualTo(programDefinition.id());
  }

  @Test
  public void submit_obsoleteProgram_redirectsToReviewPageForTi_whenFastForwardEnabled() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc")
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .buildDefinition();
    ProgramModel newProgramModel =
        resourceCreator().insertDraftProgram(programDefinition.adminName());
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    versionRepository.publishNewSynchronizedVersion();

    answer(programDefinition.id());

    var programId = programDefinition.id();

    Request request =
        fakeRequestBuilder().addCiviFormSetting("FASTFORWARD_ENABLED", "true").build();

    ApplicantModel tiApplicant = createApplicant();
    AccountModel tiAccount = createTIWithMockedProfile(tiApplicant);

    Result result =
        blockController
            .updateWithApplicantId(
                request,
                tiApplicant.id,
                programId,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation().isPresent()).isTrue();
    assertThat(result.redirectLocation().get())
        .isEqualTo(
            routes.ApplicantProgramReviewController.reviewWithApplicantId(
                    tiApplicant.id, newProgramModel.id)
                .url());

    // An application was not submitted
    ApplicationRepository applicationRepository = instanceOf(ApplicationRepository.class);
    ImmutableSet<ApplicationModel> applications =
        applicationRepository
            .getApplicationsForApplicant(tiApplicant.id, ImmutableSet.of(LifecycleStage.ACTIVE))
            .toCompletableFuture()
            .join();
    assertThat(applications).isEmpty();
  }

  @Test
  public void submit_obsoleteProgram_redirectsToReviewPage_whenFastForwardEnabled() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc")
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .buildDefinition();
    ProgramModel newProgramModel =
        resourceCreator().insertDraftProgram(programDefinition.adminName());
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    versionRepository.publishNewSynchronizedVersion();

    answer(programDefinition.id());

    var programId = programDefinition.id();

    Request request =
        fakeRequestBuilder().addCiviFormSetting("FASTFORWARD_ENABLED", "true").build();

    Result result =
        blockController
            .update(
                request,
                programId,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation().isPresent()).isTrue();
    assertThat(result.redirectLocation().get())
        .isEqualTo(routes.ApplicantProgramReviewController.review(newProgramModel.id).url());

    // An application was not submitted
    ApplicationRepository applicationRepository = instanceOf(ApplicationRepository.class);
    ImmutableSet<ApplicationModel> applications =
        applicationRepository
            .getApplicationsForApplicant(applicant.id, ImmutableSet.of(LifecycleStage.ACTIVE))
            .toCompletableFuture()
            .join();
    assertThat(applications).isEmpty();
  }

  @Test
  public void submit_isSuccessful() {
    ProgramModel activeProgram =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .withBlock()
            .withRequiredQuestion(testQuestionBank().staticContent())
            .build();
    answer(activeProgram.id);

    Result result = this.submit(applicant.id, activeProgram.id);
    assertThat(result.status()).isEqualTo(FOUND);

    // An application was submitted
    ApplicationRepository applicationRepository = instanceOf(ApplicationRepository.class);
    ImmutableSet<ApplicationModel> applications =
        applicationRepository
            .getApplicationsForApplicant(applicant.id, ImmutableSet.of(LifecycleStage.ACTIVE))
            .toCompletableFuture()
            .join();
    assertThat(applications).hasSize(1);
    assertThat(applications.asList().get(0).getProgram().id).isEqualTo(activeProgram.id);
  }

  @Test
  public void submit_incomplete_showsError() {
    ProgramModel activeProgram =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    // The questions haven't been answered.
    Result result = this.submit(applicant.id, activeProgram.id);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().get(FlashKey.ERROR)).isPresent();
    assertThat(result.flash().get(FlashKey.ERROR).get())
        .contains("There's been an update to the application");
  }

  @Test
  public void submit_duplicate_handlesErrorAndDoesNotSaveDuplicateApplication() {
    ProgramModel activeProgram =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .withBlock()
            .withRequiredQuestion(testQuestionBank().staticContent())
            .build();
    answer(activeProgram.id);
    this.submit(applicant.id, activeProgram.id);

    // Submit the application again without editing
    Result noEditsResult = this.submit(applicant.id, activeProgram.id);
    // Error is handled and applicant is shown duplicates page
    assertThat(noEditsResult.status()).isEqualTo(OK);

    // Edit the application but re-enter the same values
    answer(activeProgram.id);
    Result sameValuesResult = this.submit(applicant.id, activeProgram.id);
    // Error is handled and applicant is shown duplicates page
    assertThat(sameValuesResult.status()).isEqualTo(OK);

    // There is only one application saved in the db
    ApplicationRepository applicationRepository = instanceOf(ApplicationRepository.class);
    ImmutableSet<ApplicationModel> applications =
        applicationRepository
            .getApplicationsForApplicant(applicant.id, ImmutableSet.of(LifecycleStage.ACTIVE))
            .toCompletableFuture()
            .join();
    assertThat(applications).hasSize(1);
    assertThat(applications.asList().get(0).getProgram().id).isEqualTo(activeProgram.id);
  }

  public Result review(long applicantId, long programId) {
    Request request =
        fakeRequestBuilder()
            .call(
                routes.ApplicantProgramReviewController.reviewWithApplicantId(
                    applicantId, programId))
            .header(skipUserProfile, "false")
            .build();
    return subject
        .reviewWithApplicantId(request, applicantId, programId)
        .toCompletableFuture()
        .join();
  }

  public Result submit(long applicantId, long programId) {
    Request request =
        fakeRequestBuilder()
            .call(
                routes.ApplicantProgramReviewController.submitWithApplicantId(
                    applicantId, programId))
            .header(skipUserProfile, "false")
            .build();
    return subject
        .submitWithApplicantId(request, applicantId, programId)
        .toCompletableFuture()
        .join();
  }

  private void answer(long programId) {
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "FirstName",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    "LastName"))
            .build();

    Result result =
        blockController
            .updateWithApplicantId(
                request,
                applicant.id,
                programId,
                /* blockId= */ "1",
                /* inReview= */ false,
                new ApplicantRequestedActionWrapper())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }
}
