package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.fakeRequest;

import models.Applicant;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import support.ProgramBuilder;

public class ApplicantProgramReviewControllerTest extends WithMockedApplicantProfiles {

  private ApplicantProgramReviewController subject;
  private Program program;
  private Applicant applicant;

  @Before
  public void setUpWithFreshApplicant() {
    clearDatabase();

    subject = instanceOf(ApplicantProgramReviewController.class);
    program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withQuestion(testQuestionBank().applicantName())
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

  private Result review(long applicantId, long programId) {
    Request request = 
    addCSRFToken(fakeRequest(routes.ApplicantProgramReviewController.review(applicantId, programId)))
        .build();
    return subject
        .review(request, applicantId, programId)
        .toCompletableFuture()
        .join();
  }

  // @Test
  // public void submit_empty_returnsErrors() {

  // }

  // @Test
  // public void submit_invalid_returnsUnauthorized() {

  // }

  // @Test
  // public void submit_isSuccessful() {

  // }
}
