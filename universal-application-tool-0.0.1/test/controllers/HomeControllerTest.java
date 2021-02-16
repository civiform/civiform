package controllers;

import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.Test;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.direct.AnonymousClient;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.BasicUserProfile;
import org.pac4j.core.profile.UserProfile;
import play.Application;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import org.pac4j.core.config.Config;

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
