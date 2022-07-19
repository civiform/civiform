package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Result;
import repository.ResetPostgres;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import support.ProgramBuilder;
import views.admin.programs.ProgramStatusesView;

public class AdminProgramStatusesControllerTest extends ResetPostgres {

  private AdminProgramStatusesController controller;

  @Before
  public void setup() {
    controller = instanceOf(AdminProgramStatusesController.class);
  }

  @Test
  public void index_flagDisabled() throws ProgramNotFoundException {
    Program program = ProgramBuilder.newDraftProgram("test name", "test description").build();

    // Initialize the controller explicitly to override status tracking enablement.
    controller =
        new AdminProgramStatusesController(
            instanceOf(ProgramService.class),
            instanceOf(ProgramStatusesView.class),
            instanceOf(RequestChecker.class),
            instanceOf(FormFactory.class),
            /* statusTrackingEnabled= */ false);

    Result result = controller.index(addCSRFToken(fakeRequest()).build(), program.id);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void index_ok() throws ProgramNotFoundException {
    Program program = ProgramBuilder.newDraftProgram("test name", "test description").build();

    Result result = controller.index(addCSRFToken(fakeRequest()).build(), program.id);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("No statuses have been created yet");
  }

  @Test
  public void index_missingProgram() {
    assertThatThrownBy(() -> controller.index(addCSRFToken(fakeRequest()).build(), Long.MAX_VALUE))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void index_nonDraftProgram() {
    Program program = ProgramBuilder.newActiveProgram("test name", "test description").build();

    assertThatThrownBy(() -> controller.index(addCSRFToken(fakeRequest()).build(), program.id))
        .isInstanceOf(NotChangeableException.class);
  }

  // TODO(#2572): Add more unit tests for editing / non-draft program, etc).
}
