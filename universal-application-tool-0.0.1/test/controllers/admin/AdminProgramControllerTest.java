package controllers.admin;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

import com.google.common.collect.ImmutableMap;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import repository.WithResettingPostgresContainer;
import views.html.helper.CSRF;

public class AdminProgramControllerTest extends WithResettingPostgresContainer {

  private AdminProgramController controller;

  @Before
  public void setup() {
    controller = app.injector().instanceOf(AdminProgramController.class);
  }

  @Test
  public void listWithNoPrograms() {
    Result result = controller.list();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("UTF-8; charset=utf-8");
    assertThat(contentAsString(result)).contains("Programs");
  }

  @Test
  public void list_returnsPrograms() {
    insertProgram("one");
    insertProgram("two");

    Result result = controller.list();

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

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("New Program");
    assertThat(contentAsString(result)).contains("This is a new program");
  }

  @Test
  public void create_includesNewAndExistingProgramsInList() {
    insertProgram("Existing One");
    RequestBuilder requestBuilder =
        Helpers.fakeRequest()
            .bodyForm(
                ImmutableMap.of("name", "New Program", "description", "This is a new program"));

    Result result = controller.create(requestBuilder.build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Existing One");
    assertThat(contentAsString(result)).contains("New Program");
    assertThat(contentAsString(result)).contains("This is a new program");
  }

  private static void insertProgram(String name) {
    Program program = new Program(name, "description");
    program.save();
  }

  /**
   * For our HTTP responses, we set the charset to "UTF-8; charset=utf-8", which is not
   * understandable to {@code decodeString}. The Play {@link Helpers#contentAsString} tries to use
   * the entire charset string to decode, which fails.
   */
  private static String contentAsString(Result result) {
    return contentAsBytes(result).decodeString(UTF_8);
  }
}
