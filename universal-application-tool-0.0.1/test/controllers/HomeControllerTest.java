package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.*;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.pac4j.core.context.HttpConstants;
import play.Application;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

public class HomeControllerTest extends WithApplication {

  // Should be no need for a database here.
  protected Application provideApplication() {
    ImmutableMap<String, String> args =
        new ImmutableMap.Builder<String, String>()
            .putAll(inMemoryDatabase("default", ImmutableMap.of("MODE", "PostgreSQL")))
            .put("play.evolutions.db.default.enabled", "false")
            .build();
    return fakeApplication(args);
  }

  @Test
  public void testUnauthenticatedSecurePage() {
    Http.RequestBuilder request =
        fakeRequest(routes.HomeController.secureIndex())
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    Result result = route(app, request);
    assertThat(result.status()).isNotEqualTo(HttpConstants.OK);
    assertThat(result.status()).isEqualTo(HttpConstants.UNAUTHORIZED);
  }
}
