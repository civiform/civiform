package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.stubMessagesApi;

import java.util.Locale;
import models.Applicant;
import models.Program;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import repository.WithPostgresContainer;
import support.ProgramBuilder;

public class ApplicantProgramsControllerTest extends WithPostgresContainer {

  private ApplicantProgramsController controller;

  @Before
  public void setupController() {
    controller = instanceOf(ApplicantProgramsController.class);
  }

  @Test
  public void index_withNoPrograms_returnsEmptyResult() {
    Result result = controller.index(fakeRequest().build(), 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("utf-8");
    assertThat(contentAsString(result)).contains("Programs");
  }

  @Test
  public void index_withPrograms_returnsAllPrograms() {
    resourceCreator().insertProgram("one");
    resourceCreator().insertProgram("two");

    Result result = controller.index(fakeRequest().build(), 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("one");
    assertThat(contentAsString(result)).contains("two");
  }

  @Test
  public void index_withProgram_includesApplyButtonWithRedirect() {
    Program program = resourceCreator().insertProgram("program");

    Result result = controller.index(fakeRequest().build(), 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains(routes.ApplicantProgramsController.edit(1L, program.id).url());
  }

  @Test
  public void index_usesMessagesForUserPreferredLocale() {
    // Set the PLAY_LANG cookie
    Http.Request request =
        fakeRequest().langCookie(Locale.forLanguageTag("es-US"), stubMessagesApi()).build();

    Result result = controller.index(request, 1L).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Programas");
  }

  // TODO(https://github.com/seattle-uat/universal-application-tool/issues/224): Should redirect to
  //  next incomplete block rather than first block.
  @Test
  public void edit_withNewProgram_redirectsToFirstBlock() {
    Applicant applicant = resourceCreator().insertApplicant();
    Program program = ProgramBuilder.newProgram().build();

    Result result = controller.edit(applicant.id, program.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(routes.ApplicantProgramBlocksController.edit(applicant.id, program.id, 1).url());
  }

  // TODO(https://github.com/seattle-uat/universal-application-tool/issues/224): Should redirect to
  //  next incomplete block rather than first block.
  @Ignore
  public void edit_redirectsToFirstIncompleteBlock() {}

  // TODO(https://github.com/seattle-uat/universal-application-tool/issues/256): Should redirect to
  //  end of program submission.
  @Ignore
  public void edit_whenNoMoreIncompleteBlocks_redirectsToListOfPrograms() {
    Applicant applicant = resourceCreator().insertApplicant();
    Program program = resourceCreator().insertProgram("My Program");

    Result result = controller.edit(applicant.id, program.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation())
        .hasValue(routes.ApplicantProgramsController.index(applicant.id).url());
  }
}
