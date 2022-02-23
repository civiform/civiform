package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.FOUND;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.fakeRequest;

import controllers.WithMockedProfiles;
import models.Applicant;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import support.ProgramBuilder;

public class ApplicantProgramReviewControllerTest extends WithMockedProfiles {

  private ApplicantProgramReviewController subject;
  private Program program;
  public Applicant applicant;

  @Before
  public void setUpWithFreshApplicant() {
    resetDatabase();

    subject = instanceOf(ApplicantProgramReviewController.class);
    program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withRequiredQuestion(testQuestionBank().applicantName())
            .build();
    applicant = createApplicantWithMockedProfile();
  }

  @Test
  public void review_invalidApplicant_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    Result result = this.review(badApplicantId, program.id);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void review_toAProgramThatDoesNotExist_returns404() {
    long badProgramId = program.id + 1000;
    Result result = this.review(applicant.id, badProgramId);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void review_rendersSummaryView() {
    Result result = this.review(applicant.id, program.id);
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void submit_invalid_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    Result result = this.submit(badApplicantId, program.id);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void submit_isSuccessful() {
    Result result = this.submit(applicant.id, program.id);
    assertThat(result.status()).isEqualTo(FOUND);
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
}
