package controllers.admin;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import repository.ProgramRepository;
import repository.WithPostgresContainer;

public class AdminProgramControllerTest extends WithPostgresContainer {

  private AdminProgramController controller;

  @Before
  public void setup() {
    controller = app.injector().instanceOf(AdminProgramController.class);
  }

  @Test
  public void listWithNoPrograms_returnsExpectedHtml() {
    Result result = controller.list();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("UTF-8; charset=utf-8");
    assertThat(contentAsString(result)).contains("Programs");
  }

  @Test
  public void newOne_returnsExpectedForm() {
    RequestBuilder requestBuilder = Helpers.fakeRequest();
    requestBuilder = addCSRFToken(requestBuilder);

    Result result = controller.newOne(requestBuilder.build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Create a new Program");
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
