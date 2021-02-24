package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;

import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import repository.WithPostgresContainer;

public class ApplicantProgramsControllerTest extends WithPostgresContainer {

  private ApplicantProgramsController controller;

  @Before
  public void setupController() {
    controller = resourceFabricator().instanceOf(ApplicantProgramsController.class);
  }

  @Test
  public void index_withNoPrograms_returnsEmptyResult() {
    Result result = controller.index(1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("utf-8");
    assertThat(contentAsString(result)).contains("Programs");
  }

  @Test
  public void index_withPrograms_returnsAllPrograms() {
    resourceFabricator().insertProgram("one");
    resourceFabricator().insertProgram("two");

    Result result = controller.index(1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("one");
    assertThat(contentAsString(result)).contains("two");
  }

  @Test
  public void index_withProgram_includesApplyButtonWithRedirect() {
    Program program = resourceFabricator().insertProgram("program");

    Result result = controller.index(1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(
            controllers.applicant.routes.ApplicantProgramsController.edit(1L, program.id).url());
  }

  @Test
  public void edit_returnsTemporaryString() {
    Result result = controller.edit(123L, 456L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).isEqualTo("Applicant 123 chose program 456");
  }
}