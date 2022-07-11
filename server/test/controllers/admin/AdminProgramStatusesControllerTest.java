package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import repository.ResetPostgres;
import services.program.ProgramNotFoundException;
import support.ProgramBuilder;

public class AdminProgramStatusesControllerTest extends ResetPostgres {

  private AdminProgramStatusesController controller;

  @Before
  public void setup() {
    controller = instanceOf(AdminProgramStatusesController.class);
  }

  @Test
  public void index_ok() throws ProgramNotFoundException {
    Program program = ProgramBuilder.newDraftProgram("test name", "test description").build();

    Result result = controller.index(fakeRequest().build(), program.id);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Needs more information");
  }

  @Test
  public void index_missingProgram() throws ProgramNotFoundException {
    Result result = controller.index(fakeRequest().build(), Long.MAX_VALUE);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }
}
