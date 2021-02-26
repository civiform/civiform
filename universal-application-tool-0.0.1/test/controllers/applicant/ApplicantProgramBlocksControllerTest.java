package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.fakeRequest;

import controllers.ApplicantProgramBlocksController;
import controllers.routes;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
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
    Http.Request request =
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
    Http.Request request =
        fakeRequest(routes.ApplicantProgramBlocksController.edit(applicant.id, program.id(), 2L))
            .build();

    Result result =
        subject.edit(request, applicant.id, program.id(), 2L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }
}
