package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.FOUND;
import static play.test.Helpers.contentAsString;

import com.google.common.collect.ImmutableMap;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import repository.WithPostgresContainer;
import views.html.helper.CSRF;

public class AdminProgramControllerTest extends WithPostgresContainer {

  private AdminProgramController controller;

  @Before
  public void setup() {
    controller = app.injector().instanceOf(AdminProgramController.class);
  }

  @Test
  public void index_withNoPrograms() {
    Result result = controller.index();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("utf-8");
    assertThat(contentAsString(result)).contains("Programs");
  }

  @Test
  public void index_returnsPrograms() {
    insertProgram("one");
    insertProgram("two");

    Result result = controller.index();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("one");
    assertThat(contentAsString(result)).contains("two");
  }

  @Test
  public void newOne_returnsExpectedForm() {
    Request request = addCSRFToken(Helpers.fakeRequest()).build();

    Result result = controller.newOne(request);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Create a new Program");
    assertThat(contentAsString(result)).contains(CSRF.getToken(request.asScala()).value());
  }

  @Test
  public void create_returnsNewProgramInList() {
    RequestBuilder requestBuilder =
        Helpers.fakeRequest()
            .bodyForm(
                ImmutableMap.of("name", "New Program", "description", "This is a new program"));

    Result result = controller.create(requestBuilder.build());

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation()).hasValue(routes.AdminProgramController.index().url());

    Result redirectResult = controller.index();
    assertThat(contentAsString(redirectResult)).contains("New Program");
    assertThat(contentAsString(redirectResult)).contains("This is a new program");
  }

  @Test
  public void create_includesNewAndExistingProgramsInList() {
    insertProgram("Existing One");
    RequestBuilder requestBuilder =
        Helpers.fakeRequest()
            .bodyForm(
                ImmutableMap.of("name", "New Program", "description", "This is a new program"));

    Result result = controller.create(requestBuilder.build());

    assertThat(result.status()).isEqualTo(FOUND);
    assertThat(result.redirectLocation()).hasValue(routes.AdminProgramController.index().url());

    Result redirectResult = controller.index();
    assertThat(contentAsString(redirectResult)).contains("Existing One");
    assertThat(contentAsString(redirectResult)).contains("New Program");
    assertThat(contentAsString(redirectResult)).contains("This is a new program");
  }

  private static void insertProgram(String name) {
    Program program = new Program(name, "description");
    program.save();
  }
}
