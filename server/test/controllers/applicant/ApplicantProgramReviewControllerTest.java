package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.FOUND;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
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
import play.i18n.MessagesApi;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.ApplicationRepository;
import repository.VersionRepository;
import services.Path;
import services.applicant.ApplicantService;
import services.applicant.question.Scalar;
import services.monitoring.MonitoringMetricCounters;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.applicant.ApplicantProgramSummaryView;
import views.applicant.NorthStarApplicantIneligibleView;
import views.applicant.NorthStarApplicantProgramSummaryView;
import views.applicant.PreventDuplicateSubmissionView;

public class ApplicantProgramReviewControllerTest extends WithMockedProfiles {

  private ApplicantProgramReviewController subject;
  private ApplicantProgramBlocksController blockController;
  private ProgramModel activeProgram;
  public ApplicantModel applicant;
  private SettingsManifest settingsManifest;

  @Before
  public void setUpWithFreshApplicants() {
    resetDatabase();

    blockController = instanceOf(ApplicantProgramBlocksController.class);
    activeProgram =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    applicant = createApplicantWithMockedProfile();

    settingsManifest = mock(SettingsManifest.class);
    subject =
        new ApplicantProgramReviewController(
            instanceOf(ApplicantService.class),
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(MessagesApi.class),
            instanceOf(ApplicantProgramSummaryView.class),
            instanceOf(NorthStarApplicantProgramSummaryView.class),
            instanceOf(NorthStarApplicantIneligibleView.class),
            instanceOf(PreventDuplicateSubmissionView.class),
            instanceOf(ProfileUtils.class),
            settingsManifest,
            instanceOf(ProgramService.class),
            instanceOf(VersionRepository.class),
            instanceOf(ProgramSlugHandler.class),
            instanceOf(ApplicantRoutes.class),
            instanceOf(EligibilityAlertSettingsCalculator.class),
            instanceOf(MonitoringMetricCounters.class));
  }

  @Test
  public void review_whenProgramSlugUrlsFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    String programId = String.valueOf(activeProgram.id);

    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject.review(request, programId, /* isFromUrlCall= */ true).toCompletableFuture().join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  /**
   * Tests that review() returns OK when the feature is disabled and is from url call with a numeric
   * program id. review() also returns OK for other combinations: when the feature is disabled OR
   * when the call is not from a URL OR when the program param is a program slug (not numeric) AND
   * the program ID was properly retrieved. We don't test all combinations here because the
   * ProgramSlugHandler tests have a comprehensive test cover for them.
   */
  @Test
  public void review_whenProgramSlugUrlsFeatureDisabledAndIsProgramIdFromUrl_isOk() {
    String programId = String.valueOf(activeProgram.id);
    Result result =
        subject
            .review(fakeRequest(), programId, /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  /**
   * Tests that review() throws an error when the program param is a program slug but it should be
   * the program id since the program slug feature is disabled. review() also throws error for other
   * combinations when the program param is not properly parsed. We don't test all combinations here
   * because ProgramSlugHandler have a comprehensive test cover for them.
   */
  @Test
  public void review_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    String programSlug = activeProgram.getSlug();
    assertThatThrownBy(() -> subject.review(fakeRequest(), programSlug, /* isFromUrlCall= */ true))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from '' to a numeric value");
  }

  @Test
  public void
      reviewWithApplicanId_whenProgramSlugUrlsFeatureEnabledAndIsProgramIdFromUrl_redirectsToHome() {
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .reviewWithApplicantId(
                request, applicant.id, Long.toString(activeProgram.id), /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();

    // Redirects to home since program IDs are not supported when feature is enabled and program
    // param expects a program slug
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void reviewWithApplicantId_invalidApplicant_redirectsToHome() {
    long badApplicantId = applicant.id + 1000;
    Result result = this.reviewWithApplicantId(badApplicantId, activeProgram.id);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  @Test
  public void reviewWithApplicantId_applicantAccessToDraftProgram_redirectsToHome() {
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    Result result = this.reviewWithApplicantId(applicant.id, draftProgram.id);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue("/");
  }

  /**
   * Tests that reviewWithApplicantId() throws an error when the program param is a program slug but
   * it should be the program id since the program slug feature is disabled. reviewWithApplicantId()
   * also throws error for other combinations when the program param is not properly parsed. We
   * don't test all combinations here because ProgramSlugHandler have a comprehensive test cover for
   * them.
   */
  @Test
  public void
      reviewWithApplicantId_whenProgramSlugUrlsFeatureDisabledAndIsProgramSlugFromUrl_error() {
    String programSlug = activeProgram.getSlug();
    assertThatThrownBy(
            () ->
                subject.reviewWithApplicantId(
                    fakeRequest(), applicant.id, programSlug, /* isFromUrlCall= */ true))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from '' to a numeric value");
  }

  /**
   * Tests that reviewWithApplicantId() returns OK when the feature is enabled and is from url call
   * with a program slug. reviewWithApplicantId() also returns OK for other combinations: when the
   * feature is disabled OR when the call is not from a URL OR when the program param is a program
   * slug (not numeric), AND the program ID was properly retrieved. We don't test all combinations
   * here because the ProgramSlugHandler tests have a comprehensive test cover for them.
   */
  @Test
  public void
      reviewWithApplicantId_whenProgramSlugUrlsFeatureEnabledAndIsProgramSlugFromUrl_isOk() {
    String programSlug = activeProgram.getSlug();
    Request request = fakeRequestBuilder().build();
    when(this.settingsManifest.getProgramSlugUrlsEnabled(request)).thenReturn(true);

    Result result =
        subject
            .reviewWithApplicantId(request, applicant.id, programSlug, /* isFromUrlCall= */ true)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void reviewWithApplicantId_civiformAdminAccessToDraftProgram_isOk() {
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.representativeApplicant().orElseThrow();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .build();
    Result result = this.reviewWithApplicantId(applicant.id, draftProgram.id);
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void reviewWithApplicantId_obsoleteProgram_isOk() {
    ProgramModel obsoleteProgram = ProgramBuilder.newObsoleteProgram("program").build();
    Result result = this.reviewWithApplicantId(applicant.id, obsoleteProgram.id);
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void reviewWithApplicantId_toAProgramThatDoesNotExist_returns404() {
    long badProgramId = activeProgram.id + 1000;
    Result result = this.reviewWithApplicantId(applicant.id, badProgramId);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void reviewWithApplicantId_rendersSummaryView() {
    Result result = this.reviewWithApplicantId(applicant.id, activeProgram.id);
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
    applicant = adminAccount.representativeApplicant().orElseThrow();

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
  public void submit_obsoleteProgram_redirectsToReviewPageForTi() {
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

    ApplicantModel tiApplicant = createApplicant();
    createTIWithMockedProfile(tiApplicant);

    Result result =
        blockController
            .updateWithApplicantId(
                fakeRequest(),
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
                    tiApplicant.id, Long.toString(newProgramModel.id), /* isFromUrlCall= */ false)
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
  public void submit_obsoleteProgram_redirectsToReviewPage() {
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

    Result result =
        blockController
            .update(
                fakeRequest(),
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
            routes.ApplicantProgramReviewController.review(
                    Long.toString(newProgramModel.id), /* isFromUrlCall= */ false)
                .url());

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

  @Test
  public void northstar_submit_duplicate_handlesErrorAndDoesNotSaveDuplicateApplication() {
    ProgramModel activeProgram =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().nameApplicantName())
            .withBlock()
            .withRequiredQuestion(testQuestionBank().staticContent())
            .build();
    answer(activeProgram.id);
    this.northstarSubmit(applicant.id, activeProgram.id);

    // Submit the application again without editing
    Result noEditsResult = this.northstarSubmit(applicant.id, activeProgram.id);
    // Error is handled and applicant is shown duplicates page
    assertThat(noEditsResult.status()).isEqualTo(FOUND);

    // Edit the application but re-enter the same values
    answer(activeProgram.id);
    Result sameValuesResult = this.northstarSubmit(applicant.id, activeProgram.id);
    // Error is handled and applicant is shown duplicates page
    assertThat(sameValuesResult.status()).isEqualTo(FOUND);

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

  public Result reviewWithApplicantId(long applicantId, long programId) {
    String programIdStr = String.valueOf(programId);
    Request request =
        fakeRequestBuilder()
            .call(
                routes.ApplicantProgramReviewController.reviewWithApplicantId(
                    applicantId, programIdStr, /* isFromUrlCall= */ false))
            .header(skipUserProfile, "false")
            .build();
    return subject
        .reviewWithApplicantId(request, applicantId, programIdStr, /* isFromUrlCall= */ false)
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
    when(settingsManifest.getNorthStarApplicantUi()).thenReturn(false);
    return subject
        .submitWithApplicantId(request, applicantId, programId)
        .toCompletableFuture()
        .join();
  }

  public Result northstarSubmit(long applicantId, long programId) {
    Request request =
        fakeRequestBuilder()
            .call(
                routes.ApplicantProgramReviewController.submitWithApplicantId(
                    applicantId, programId))
            .header(skipUserProfile, "false")
            .build();
    when(settingsManifest.getNorthStarApplicantUi()).thenReturn(true);
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
