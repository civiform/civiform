package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.fakeRequest;

import org.junit.Test;
import org.pac4j.core.context.HttpConstants;
import play.mvc.Http;
import play.mvc.Result;
import repository.ResetPostgres;
import support.CfTestHelpers;
import support.CfTestHelpers.ResultWithFinalRequestUri;

public class HomeControllerTest extends ResetPostgres {

  @Test
  public void testUnauthenticatedSecurePage() {
    Http.RequestBuilder request =
        fakeRequest(routes.HomeController.securePlayIndex())
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    ResultWithFinalRequestUri resultWithFinalRequestUri =
        CfTestHelpers.doRequestWithInternalRedirects(app, request);
    assertThat(resultWithFinalRequestUri.getResult().status()).isEqualTo(HttpConstants.OK);

    assertThat(resultWithFinalRequestUri.getFinalRequestUri()).startsWith("/applicants/");
    assertThat(resultWithFinalRequestUri.getFinalRequestUri()).endsWith("/programs");
  }

  @Test
  public void testFavicon() {
    Http.RequestBuilder request =
        fakeRequest(routes.HomeController.favicon())
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    ResultWithFinalRequestUri resultWithFinalRequestUri =
        CfTestHelpers.doRequestWithInternalRedirects(app, request);
    Result result = resultWithFinalRequestUri.getResult();
    assertThat(result.redirectLocation()).isNotEmpty();
    assertThat(result.redirectLocation().get()).contains("civiform.us/favicon");
  }
}
