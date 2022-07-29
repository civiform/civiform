package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.UNAUTHORIZED;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.test.Helpers;
import repository.ResetPostgres;
import support.ProgramBuilder;

public class AdminApplicationControllerTest extends ResetPostgres {
  // NOTE: the controller asserts the user is valid on the program that applications are requested
  // for. However, we currently have no pattern for setting a profile in a test request, so we can't
  // make affirmative tests.
  private AdminApplicationController controller;

  @Before
  public void setupController() {
    controller = instanceOf(AdminApplicationController.class);
  }

  @Test
  public void index_noUser_errors() throws Exception {
    long programId = ProgramBuilder.newActiveProgram().buildDefinition().id();
    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    Result result =
        controller.index(
            request,
            programId,
            /* search= */ Optional.empty(),
            /* page= */ Optional.of(1), // Needed to skip redirect.
            /* fromDate= */ Optional.empty(),
            /* untilDate= */ Optional.empty());
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }
}
