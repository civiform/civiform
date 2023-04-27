package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

import featureflags.FeatureFlag;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pac4j.core.context.HttpConstants;
import play.mvc.Http;
import play.mvc.Result;
import repository.ResetPostgres;

@RunWith(JUnitParamsRunner.class)
public class HomeControllerTest extends ResetPostgres {

  @Test
  @Parameters({"true", "false"})
  public void testUnauthenticatedSecurePage(boolean bypassLoginLanguageScreens) {
    Http.RequestBuilder request =
        fakeRequest(routes.HomeController.securePlayIndex())
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort())
            .session(
                FeatureFlag.BYPASS_LOGIN_LANGUAGE_SCREENS.toString(),
                Boolean.toString(bypassLoginLanguageScreens));
    Result result = route(app, request);
    assertThat(result.status()).isNotEqualTo(HttpConstants.OK);

    if (bypassLoginLanguageScreens) {
      assertThat(result.redirectLocation().get()).isEqualTo(routes.HomeController.index().url());
    } else {
      assertThat(result.redirectLocation())
          .contains(routes.HomeController.loginForm(Optional.of("login")).url());
    }
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
    assertThat(result.redirectLocation().get())
        .as("Should redirect to set favicon")
        .contains("civiform.us");
  }
}
