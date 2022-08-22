package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

import java.util.Optional;
import org.junit.Test;
import org.pac4j.core.context.HttpConstants;
import play.mvc.Http;
import play.mvc.Result;
import repository.ResetPostgres;

public class HomeControllerTest extends ResetPostgres {

  @Test
  public void testUnauthenticatedSecurePage() {
    Http.RequestBuilder request =
        fakeRequest(routes.HomeController.securePlayIndex())
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    Result result = route(app, request);
    assertThat(result.status()).isNotEqualTo(HttpConstants.OK);
    assertThat(result.redirectLocation())
        .contains(routes.HomeController.loginForm(Optional.of("login")).url());
  }

  @Test
  public void testFavicon() {
    Http.RequestBuilder request =
        fakeRequest(routes.HomeController.favicon())
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    Result result = route(app, request);
    assertThat(result.status())
        .as("Result status should 302 redirect")
        .isEqualTo(HttpConstants.FOUND);
    assertThat(result.redirectLocation().isPresent()).as("Should have redirect location").isTrue();
    ;
    assertThat(result.redirectLocation().get())
        .as("Should redirect to set favicon")
        .contains("civiform.us");
  }
}
