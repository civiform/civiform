package app;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.route;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.context.HttpConstants;
import play.mvc.Http;
import play.mvc.Result;
import repository.ResetPostgres;

/**
 * Test for asserting path-based authentication logic as specified by pac4j.security.rules value in
 * application.conf
 */
public class ApiAuthenticationTest extends ResetPostgres {
  private static final String keyId = "keyId";
  private static final String secret = "secret";
  public static final String API_CHECK_URL =
      controllers.api.routes.CiviFormApiController.checkAuth().url();

  @Before
  public void setUp() {
    resourceCreator.createActiveApiKey("test-key", keyId, secret);
  }

  @Test
  public void nonApiRoutes_doNotRequireApiAuth() {
    Result result =
        doGetRequest(
            fakeRequestBuilder()
                .method("GET")
                .uri(controllers.routes.HomeController.index().url()));
    // The HomeController index page redirects to the programs page.
    assertThat(result.status()).isEqualTo(HttpConstants.SEE_OTHER);
  }

  @Test
  public void apiRoutes_success() {
    String rawCredentials = keyId + ":" + secret;
    String creds =
        Base64.getEncoder().encodeToString(rawCredentials.getBytes(StandardCharsets.UTF_8));

    Result result =
        doGetRequest(
            fakeRequestBuilder()
                .method("GET")
                .uri(API_CHECK_URL)
                .remoteAddress("1.1.1.1")
                .header("Authorization", "Basic " + creds));

    assertThat(result.status()).isEqualTo(HttpConstants.OK);
  }

  @Test
  public void apiRoutes_invalidKey() {
    String rawCredentials = keyId + ":" + "nottherealsecret";
    String creds =
        Base64.getEncoder().encodeToString(rawCredentials.getBytes(StandardCharsets.UTF_8));

    Result result =
        doGetRequest(
            fakeRequestBuilder()
                .method("GET")
                .uri(API_CHECK_URL)
                .remoteAddress("1.1.1.1")
                .header("Authorization", "Basic " + creds));

    assertThat(result.status()).isEqualTo(HttpConstants.UNAUTHORIZED);
  }

  private Result doGetRequest(Http.RequestBuilder request) {
    return route(app, request.header(Http.HeaderNames.HOST, "localhost:" + testServerPort()));
  }
}
