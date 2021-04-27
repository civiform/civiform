package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.Applicant;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;

public class ApplicantProgramBlocksControllerTest extends WithMockedApplicantProfiles {

  private ApplicantProgramBlocksController subject;
  private Program program;
  private Applicant applicant;

  @Before
  public void setUpWithFreshApplicant() {
    clearDatabase();

    subject = instanceOf(ApplicantProgramBlocksController.class);
    program =
        ProgramBuilder.newDraftProgram()
            .withBlock()
            .withQuestion(testQuestionBank().applicantName())
            .build();
    applicant = createApplicantWithMockedProfile();
  }

  @Test
  public void edit_invalidApplicant_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    Request request =
        fakeRequest(routes.ApplicantProgramBlocksController.edit(badApplicantId, program.id, "1"))
            .build();

    Result result =
        subject.edit(request, badApplicantId, program.id, "1").toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void edit_toAProgramThatDoesNotExist_returns404() {
    Request request =
        addCSRFToken(
                fakeRequest(
                    routes.ApplicantProgramBlocksController.edit(
                        applicant.id, program.id + 1000, "1")))
            .build();

    Result result =
        subject.edit(request, applicant.id, program.id + 1000, "1").toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_toAnExistingBlock_rendersTheBlock() {
    Request request =
        addCSRFToken(
                fakeRequest(
                    routes.ApplicantProgramBlocksController.edit(applicant.id, program.id, "1")))
            .build();

    Result result =
        subject.edit(request, applicant.id, program.id, "1").toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void edit_toABlockThatDoesNotExist_returns404() {
    Request request =
        fakeRequest(routes.ApplicantProgramBlocksController.edit(applicant.id, program.id, "2"))
            .build();

    Result result =
        subject.edit(request, applicant.id, program.id, "2").toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void update_invalidApplicant_returnsUnauthorized() {
    long badApplicantId = applicant.id + 1000;
    Request request =
        fakeRequest(
                routes.ApplicantProgramBlocksController.update(
                    badApplicantId,
                    program.id,
                    /** blockId = */
                    "1",
                    /** inReview = */
                    false))
            .build();

    Result result =
        subject
            .update(
                request,
                badApplicantId,
                program.id,
                /** blockId = */
                "1",
                /** inReview = */
                false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void update_invalidProgram_returnsBadRequest() {
    long badProgramId = program.id + 1000;
    Request request =
        fakeRequest(
                routes.ApplicantProgramBlocksController.update(
                    applicant.id,
                    badProgramId,
                    /** blockId = */
                    "1",
                    /** inReview = */
                    false))
            .build();

    Result result =
        subject
            .update(
                request,
                applicant.id,
                badProgramId,
                /** blockId = */
                "1",
                /** inReview = */
                false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_invalidBlock_returnsBadRequest() {
    String badBlockId = "1000";
    Request request =
        fakeRequest(
                routes.ApplicantProgramBlocksController.update(
                    applicant.id,
                    program.id,
                    badBlockId,
                    /** inReview = */
                    false))
            .build();

    Result result =
        subject
            .update(
                request,
                applicant.id,
                program.id,
                badBlockId,
                /** inReview = */
                false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_invalidPathsInRequest_returnsBadRequest() {
    Request request =
        fakeRequest(
                routes.ApplicantProgramBlocksController.update(
                    applicant.id,
                    program.id,
                    /** blockId = */
                    "1",
                    /** inReview = */
                    false))
            .bodyForm(ImmutableMap.of("fake.path", "value"))
            .build();

    Result result =
        subject
            .update(
                request,
                applicant.id,
                program.id,
                /** blockId = */
                "1",
                /** inReview = */
                false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_reservedPathsInRequest_returnsBadRequest() {
    String reservedPath = "metadata." + QuestionDefinition.METADATA_UPDATE_PROGRAM_ID_KEY;
    Request request =
        fakeRequest(
                routes.ApplicantProgramBlocksController.update(
                    applicant.id,
                    program.id,
                    /** blockId = */
                    "1",
                    /** inReview = */
                    false))
            .bodyForm(ImmutableMap.of(reservedPath, "value"))
            .build();

    Result result =
        subject
            .update(
                request,
                applicant.id,
                program.id,
                /** blockId = */
                "1",
                /** inReview = */
                false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_withValidationErrors_isOK() {
    Request request =
        addCSRFToken(
                fakeRequest(
                        routes.ApplicantProgramBlocksController.update(
                            applicant.id,
                            program.id,
                            /** blockId = */
                            "1",
                            /** inReview = */
                            false))
                    .bodyForm(
                        ImmutableMap.of(
                            "applicant.applicant_name.first_name",
                            "FirstName",
                            "applicant.applicant_name.last_name",
                            "")))
            .build();

    Result result =
        subject
            .update(
                request,
                applicant.id,
                program.id,
                /** blockId = */
                "1",
                /** inReview = */
                false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("FirstName");
    assertThat(contentAsString(result)).contains("Last name is required.");
  }

  @Test
  public void update_withNextBlock_redirectsToEdit() {
    program =
        ProgramBuilder.newDraftProgram()
            .withBlock("block 1")
            .withQuestion(testQuestionBank().applicantName())
            .withBlock("block 2")
            .withQuestion(testQuestionBank().applicantAddress())
            .build();
    Request request =
        fakeRequest(
                routes.ApplicantProgramBlocksController.update(
                    applicant.id,
                    program.id,
                    /** blockId = */
                    "1",
                    /** inReview = */
                    false))
            .bodyForm(
                ImmutableMap.of(
                    "applicant.applicant_name.first_name",
                    "FirstName",
                    "applicant.applicant_name.last_name",
                    "LastName"))
            .build();

    Result result =
        subject
            .update(
                request,
                applicant.id,
                program.id,
                /** blockId = */
                "1",
                /** inReview = */
                false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String nextBlockEditRoute =
        routes.ApplicantProgramBlocksController.edit(
                applicant.id,
                program.id,
                /** blockId = */
                "2")
            .url();
    assertThat(result.redirectLocation()).hasValue(nextBlockEditRoute);
  }

  @Test
  public void update_completedProgram_redirectsToReviewPage() {
    program =
        ProgramBuilder.newDraftProgram()
            .withBlock("block 1")
            .withQuestion(testQuestionBank().applicantName())
            .build();

    Request request =
        fakeRequest(
                routes.ApplicantProgramBlocksController.update(
                    applicant.id,
                    program.id,
                    /** blockId = */
                    "1",
                    /** inReview = */
                    false))
            .bodyForm(
                ImmutableMap.of(
                    "applicant.applicant_name.first_name",
                    "FirstName",
                    "applicant.applicant_name.last_name",
                    "LastName"))
            .build();

    Result result =
        subject
            .update(
                request,
                applicant.id,
                program.id,
                /** blockId = */
                "1",
                /** inReview = */
                false)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    String reviewRoute =
        routes.ApplicantProgramReviewController.review(applicant.id, program.id).url();

    assertThat(result.redirectLocation()).hasValue(reviewRoute);
  }

  @Test
  public void edit_withMessages_returnsCorrectButtonText() {
    Request request =
        addCSRFToken(
                fakeRequest(
                        routes.ApplicantProgramBlocksController.edit(applicant.id, program.id, "1"))
                    .langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi()))
            .build();

    Result result =
        subject
            .edit(
                request,
                applicant.id,
                program.id,
                /** blockId = */
                "1")
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Guardar y continuar");
  }
}
