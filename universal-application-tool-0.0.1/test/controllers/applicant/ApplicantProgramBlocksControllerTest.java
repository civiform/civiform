package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
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
import repository.WithPostgresContainer;
import support.ProgramBuilder;
import support.Questions;

public class ApplicantProgramBlocksControllerTest extends WithPostgresContainer {

  private ApplicantProgramBlocksController subject;
  private Program program;
  private Applicant applicant;

  @Before
  public void setUp() {
    subject = instanceOf(ApplicantProgramBlocksController.class);
    program =
        ProgramBuilder.newProgram().withBlock().withQuestion(Questions.applicantName()).build();
    applicant = resourceCreator().insertApplicant();
  }

  @Test
  public void edit_toAProgramThatDoesNotExist_returns404() {
    Http.Request request =
        fakeRequest(routes.ApplicantProgramBlocksController.edit(applicant.id, 2L, 1L)).build();

    Result result = subject.edit(request, applicant.id, 2L, 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void edit_toAnExistingBlock_rendersTheBlock() {
    Request request =
        addCSRFToken(
                fakeRequest(
                    routes.ApplicantProgramBlocksController.edit(applicant.id, program.id, 1L)))
            .build();

    Result result =
        subject.edit(request, applicant.id, program.id, 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void edit_toABlockThatDoesNotExist_returns404() {
    Request request =
        fakeRequest(routes.ApplicantProgramBlocksController.edit(applicant.id, program.id, 2L))
            .build();

    Result result =
        subject.edit(request, applicant.id, program.id, 2L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void update_invalidApplicant_returnsBadRequest() {
    long badApplicantId = applicant.id + 1000;
    Request request =
        fakeRequest(routes.ApplicantProgramBlocksController.update(badApplicantId, program.id, 1L))
            .build();

    Result result =
        subject.update(request, badApplicantId, program.id, 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_invalidProgram_returnsBadRequest() {
    long badProgramId = program.id + 1000;
    Request request =
        fakeRequest(routes.ApplicantProgramBlocksController.update(applicant.id, badProgramId, 1L))
            .build();

    Result result =
        subject.update(request, applicant.id, badProgramId, 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_invalidBlock_returnsBadRequest() {
    long badBlockId = 1000;
    Request request =
        fakeRequest(
                routes.ApplicantProgramBlocksController.update(
                    applicant.id, program.id, badBlockId))
            .build();

    Result result =
        subject.update(request, applicant.id, program.id, badBlockId).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_invalidPathsInRequest_returnsBadRequest() {
    Request request =
        fakeRequest(routes.ApplicantProgramBlocksController.update(applicant.id, program.id, 1L))
            .bodyForm(ImmutableMap.of("fake.path", "value"))
            .build();

    Result result =
        subject.update(request, applicant.id, program.id, 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_withValidationErrors_isOK() {
    Request request =
        addCSRFToken(
                fakeRequest(
                        routes.ApplicantProgramBlocksController.update(
                            applicant.id, program.id, 1L))
                    .bodyForm(
                        ImmutableMap.of(
                            "applicant.name.first", "FirstName", "applicant.name.last", "")))
            .build();

    Result result =
        subject.update(request, applicant.id, program.id, 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("FirstName");
    assertThat(contentAsString(result)).contains("Last name is required.");
  }

  @Test
  public void update_withNextBlock_redirectsToEdit() {
    program =
        ProgramBuilder.newProgram()
            .withBlock("block 1")
            .withQuestion(Questions.applicantName())
            .withBlock("block 2")
            .withQuestion(Questions.applicantAddress())
            .build();
    Request request =
        fakeRequest(routes.ApplicantProgramBlocksController.update(applicant.id, program.id, 1L))
            .bodyForm(
                ImmutableMap.of(
                    "applicant.name.first", "FirstName", "applicant.name.last", "LastName"))
            .build();

    Result result =
        subject.update(request, applicant.id, program.id, 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    String nextBlockEditRoute =
        routes.ApplicantProgramBlocksController.edit(applicant.id, program.id, 2L).url();
    assertThat(result.redirectLocation()).hasValue(nextBlockEditRoute);
  }

  @Test
  public void update_completedProgram_redirectsToReviewPage() {
    program =
        ProgramBuilder.newProgram()
            .withBlock("block 1")
            .withQuestion(Questions.applicantName())
            .build();

    Request request =
        fakeRequest(routes.ApplicantProgramBlocksController.update(applicant.id, program.id, 1L))
            .bodyForm(
                ImmutableMap.of(
                    "applicant.name.first", "FirstName", "applicant.name.last", "LastName"))
            .build();

    Result result =
        subject.update(request, applicant.id, program.id, 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    // TODO(https://github.com/seattle-uat/universal-application-tool/issues/256): Change
    //  reviewRoute when review page is available.
    String reviewRoute = routes.ApplicantProgramsController.index(applicant.id).url();

    assertThat(result.redirectLocation()).hasValue(reviewRoute);
  }

  @Test
  public void edit_withMessages_returnsCorrectButtonText() {
    Request request =
        addCSRFToken(
                fakeRequest(
                        routes.ApplicantProgramBlocksController.edit(applicant.id, program.id, 1L))
                    .langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi()))
            .build();

    Result result =
        subject.edit(request, applicant.id, program.id, 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Guardar y continuar");
  }
}
