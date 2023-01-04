package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableMap;
import controllers.WithMockedProfiles;
import models.Applicant;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.Path;
import services.applicant.question.Scalar;
import support.ProgramBuilder;

public class ApplicantProgramReviewControllerTest extends WithMockedProfiles {

  private ApplicantProgramReviewController subject;
  private ApplicantProgramBlocksController blockController;
  private Program draftProgram;
  public Applicant applicant;

  @Before
  public void setUpWithFreshApplicant() {
    resetDatabase();

    subject = instanceOf(ApplicantProgramReviewController.class);
    blockController = instanceOf(ApplicantProgramBlocksController.class);
    draftProgram =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();
    applicant = createApplicantWithMockedProfile();
  }

  @Test
  public void review_invalidApplicant_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    Result result = this.review(badApplicantId, draftProgram.id);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void review_toAProgramThatDoesNotExist_returns404() {
    long badProgramId = draftProgram.id + 1000;
    Result result = this.review(applicant.id, badProgramId);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void review_rendersSummaryView() {
    Result result = this.review(applicant.id, draftProgram.id);
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void submit_invalid_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    Result result = this.submit(badApplicantId, draftProgram.id);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void submit_isSuccessful() {
    Program activeProgram =
        ProgramBuilder.newActiveProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();
    answer(activeProgram.id);
    Result result = this.submit(applicant.id, activeProgram.id);
    assertThat(result.status()).isEqualTo(FOUND);
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
    assertThat(result.flash().get("error")).contains("Your application is out of date.");
  }

  public Result review(long applicantId, long programId) {
    Request request =
        addCSRFToken(
                fakeRequest(routes.ApplicantProgramReviewController.review(applicantId, programId)))
            .build();
    return subject.review(request, applicantId, programId).toCompletableFuture().join();
  }

  public Result submit(long applicantId, long programId) {
    Request request =
        addCSRFToken(
                fakeRequest(routes.ApplicantProgramReviewController.submit(applicantId, programId)))
            .build();
    return subject.submit(request, applicantId, programId).toCompletableFuture().join();
  }

  private void answer(long programId) {

    Request request =
        fakeRequest(
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
