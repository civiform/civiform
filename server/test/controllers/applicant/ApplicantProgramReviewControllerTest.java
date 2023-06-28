package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.FOUND;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static support.CfTestHelpers.requestBuilderWithSettings;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import controllers.WithMockedProfiles;
import models.Account;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.Program;
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
  private Program activeProgram;
  public Applicant applicant;

  @Before
  public void setUpWithFreshApplicant() {
    resetDatabase();

    subject = instanceOf(ApplicantProgramReviewController.class);
    blockController = instanceOf(ApplicantProgramBlocksController.class);
    activeProgram =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();
    applicant = createApplicantWithMockedProfile();
  }

  @Test
  public void review_invalidApplicant_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    Result result = this.review(badApplicantId, activeProgram.id);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void review_applicantAccessToDraftProgram_returnsUnauthorized() {
    Program draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();
    Result result = this.review(applicant.id, draftProgram.id);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void review_civiformAdminAccessToDraftProgram_isOk() {
    Account adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.newestApplicant().orElseThrow();
    Program draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();
    Result result = this.review(applicant.id, draftProgram.id);
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void review_obsoleteProgram_isOk() {
    Program obsoleteProgram = ProgramBuilder.newObsoleteProgram("program").build();
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
  public void submit_invalid_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    Result result = this.submit(badApplicantId, activeProgram.id);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void submit_applicantAccessToDraftProgram_returnsUnauthorized() {
    Program draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();
    Result result = this.submit(applicant.id, draftProgram.id);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void submit_civiformAdminAccessToDraftProgram_redirectsAndDoesNotSubmitApplication() {
    Account adminAccount = createGlobalAdminWithMockedProfile();
    applicant = adminAccount.newestApplicant().orElseThrow();

    ProgramBuilder.newActiveProgram("test program", "desc")
        .withBlock()
        .withRequiredQuestion(testQuestionBank().applicantName())
        .buildDefinition();
    ProgramDefinition draftProgramDefinition =
        ProgramBuilder.newDraftProgram("test program")
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .buildDefinition();
    answer(draftProgramDefinition.id());

    Result result = this.submit(applicant.id, draftProgramDefinition.id());
    assertThat(result.status()).isEqualTo(SEE_OTHER);

    // No application was submitted
    ApplicationRepository applicationRepository = instanceOf(ApplicationRepository.class);
    ImmutableSet<Application> applications =
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
  public void submit_obsoleteProgram_isSuccessful() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc")
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .buildDefinition();
    resourceCreator().insertDraftProgram(programDefinition.adminName());
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    versionRepository.publishNewSynchronizedVersion();

    answer(programDefinition.id());

    Result result = this.submit(applicant.id, programDefinition.id());
    assertThat(result.status()).isEqualTo(FOUND);

    // An application was submitted
    ApplicationRepository applicationRepository = instanceOf(ApplicationRepository.class);
    ImmutableSet<Application> applications =
        applicationRepository
            .getApplicationsForApplicant(applicant.id, ImmutableSet.of(LifecycleStage.ACTIVE))
            .toCompletableFuture()
            .join();
    assertThat(applications).hasSize(1);
    assertThat(applications.asList().get(0).getProgram().id).isEqualTo(programDefinition.id());
  }

  @Test
  public void submit_isSuccessful() {
    Program activeProgram =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .withBlock()
            .withRequiredQuestion(testQuestionBank().staticContent())
            .build();
    answer(activeProgram.id);

    Result result = this.submit(applicant.id, activeProgram.id);
    assertThat(result.status()).isEqualTo(FOUND);

    // An application was submitted
    ApplicationRepository applicationRepository = instanceOf(ApplicationRepository.class);
    ImmutableSet<Application> applications =
        applicationRepository
            .getApplicationsForApplicant(applicant.id, ImmutableSet.of(LifecycleStage.ACTIVE))
            .toCompletableFuture()
            .join();
    assertThat(applications).hasSize(1);
    assertThat(applications.asList().get(0).getProgram().id).isEqualTo(activeProgram.id);
  }

  @Test
  public void submit_incomplete_showsError() {
    Program activeProgram =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();
    // The questions haven't been answered.
    Result result = this.submit(applicant.id, activeProgram.id);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().get("error")).isPresent();
    assertThat(result.flash().get("error").get())
        .contains("There's been an update to the application");
  }

  public Result review(long applicantId, long programId) {
    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramReviewController.review(applicantId, programId)))
            .build();
    return subject.review(request, applicantId, programId).toCompletableFuture().join();
  }

  public Result submit(long applicantId, long programId) {
    Request request =
        addCSRFToken(
                requestBuilderWithSettings(
                    routes.ApplicantProgramReviewController.submit(applicantId, programId)))
            .build();
    return subject.submit(request, applicantId, programId).toCompletableFuture().join();
  }

  private void answer(long programId) {

    Request request =
        requestBuilderWithSettings(
                routes.ApplicantProgramBlocksController.update(
                    applicant.id, programId, /* blockId = */ "1", /* inReview = */ false))
            .bodyForm(
                ImmutableMap.of(
                    Path.create("applicant.applicant_name").join(Scalar.FIRST_NAME).toString(),
                    "FirstName",
                    Path.create("applicant.applicant_name").join(Scalar.LAST_NAME).toString(),
                    "LastName"))
            .build();

    Result result =
        blockController
            .update(request, applicant.id, programId, /* blockId = */ "1", /* inReview = */ false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }
}
