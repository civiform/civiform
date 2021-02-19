package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.*;

import org.junit.Test;
import org.pac4j.core.context.HttpConstants;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

public class HomeControllerTest extends WithApplication {

  @Test
  public void testUnauthenticatedSecurePage() {
    Http.RequestBuilder request =
        fakeRequest(routes.HomeController.secureIndex())
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    Result result = route(app, request);
    assertThat(result.status()).isNotEqualTo(HttpConstants.OK);
    assertThat(result.redirectLocation()).contains("/loginForm?message=login");
  }
}
