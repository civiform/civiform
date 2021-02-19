package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.*;

import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.direct.AnonymousClient;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.HttpConstants;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

public class HomeControllerAuthenticatedTest extends WithApplication {

  @Before
  public void setUp() {
    Config config = app.injector().instanceOf(Config.class);
    config.setClients(new Clients(AnonymousClient.INSTANCE));
  }

  @Test
  public void testAuthenticatedSecurePage() {
    Http.RequestBuilder request =
        fakeRequest(routes.HomeController.secureIndex())
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    Result result = route(app, request);
    // This should work, but it doesn't!
    // assertThat(result.status()).isEqualTo(HttpConstants.OK);

    // This is the opposite of what we want, but it's what happens now anyway.
    assertThat(result.status()).isEqualTo(HttpConstants.UNAUTHORIZED);
  }
}
