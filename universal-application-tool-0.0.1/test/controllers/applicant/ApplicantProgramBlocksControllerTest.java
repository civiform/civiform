package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableMap;
import models.Applicant;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.WithPostgresContainer;
import services.program.ProgramDefinition;

public class ApplicantProgramBlocksControllerTest extends WithPostgresContainer {

  private ApplicantProgramBlocksController subject;
  private ProgramDefinition program;
  private Applicant applicant;

  @Before
  public void setUp() {
    subject = instanceOf(ApplicantProgramBlocksController.class);
    program = resourceCreator().insertProgramWithOneBlock("Test program");
    applicant = resourceCreator().insertApplicant();
  }

  @Test
  public void edit_toAnExistingBlock_rendersTheBlock() {
    Request request =
        addCSRFToken(
                fakeRequest(
                    routes.ApplicantProgramBlocksController.edit(applicant.id, program.id(), 1L)))
            .build();

    Result result =
        subject.edit(request, applicant.id, program.id(), 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void edit_toABlockThatDoesNotExist_returns404() {
    Request request =
        fakeRequest(routes.ApplicantProgramBlocksController.edit(applicant.id, program.id(), 2L))
            .build();

    Result result =
        subject.edit(request, applicant.id, program.id(), 2L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void update_invalidApplicant_returnsBadRequest() {
    long badApplicantId = applicant.id + 1000;
    Request request =
        fakeRequest(
                routes.ApplicantProgramBlocksController.update(badApplicantId, program.id(), 1L))
            .build();

    Result result =
        subject.update(request, badApplicantId, program.id(), 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_invalidProgram_returnsBadRequest() {
    long badProgramId = program.id() + 1000;
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
                    applicant.id, program.id(), badBlockId))
            .build();

    Result result =
        subject
            .update(request, applicant.id, program.id(), badBlockId)
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_invalidPathsInRequest_returnsBadRequest() {
    Request request =
        fakeRequest(routes.ApplicantProgramBlocksController.update(applicant.id, program.id(), 1L))
            .bodyForm(ImmutableMap.of("fake.path", "value"))
            .build();

    Result result =
        subject.update(request, applicant.id, program.id(), 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_withValidationErrors_isOK() {
    ;
  }

  @Test
  public void update_withNextBlock_redirectsToEdit() {
    Request request =
        fakeRequest(routes.ApplicantProgramBlocksController.update(applicant.id, program.id(), 1L))
            .bodyForm(ImmutableMap.of("applicant.my.path.name", "first name"))
            .build();

    Result result =
        subject.update(request, applicant.id, program.id(), 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    // TODO: change nextBlockEditRoute when ReadOnlyApplicantService.getFirstIncompleteBlock is
    // implemented.
    String nextBlockEditRoute =
        routes.ApplicantProgramBlocksController.edit(applicant.id, program.id(), 1L).url();
    assertThat(result.redirectLocation()).hasValue(nextBlockEditRoute);
  }

  // TODO: update this test when ReadOnlyApplicantService.getFirstIncompleteBlock is implemented.
  @Ignore
  public void update_completedProgram_redirectsToReviewPage() {}
}
