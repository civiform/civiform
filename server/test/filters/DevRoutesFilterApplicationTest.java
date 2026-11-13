package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.test.Helpers.route;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import repository.ResetPostgres;

/**
 * Checks that {@link DevRoutesFilter} is wired into the application's filter chain. It is the only
 * thing that keeps the /dev routes, which have no login, off deployments that are neither dev nor
 * staging. The default test application is neither.
 */
public class DevRoutesFilterApplicationTest extends ResetPostgres {

  @Test
  public void devPage_outsideDevAndStaging_returnsNotFound() {
    Result result = route(app, request("GET", "/dev/seed"));

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void headlessSeedAndClearRoutes_outsideDevAndStaging_returnNotFound() {
    String[] headlessRoutes = {
      "/dev/seedQuestionsHeadless",
      "/dev/seedProgramsHeadless",
      "/dev/seedApplicationsHeadless",
      "/dev/seed/clearHeadless"
    };

    for (String path : headlessRoutes) {
      Result result = route(app, request("POST", path));

      assertThat(result.status()).as("POST " + path).isEqualTo(NOT_FOUND);
    }
  }

  @Test
  public void normalPage_outsideDevAndStaging_isNotBlocked() {
    Result result = route(app, request("GET", "/"));

    assertThat(result.status()).isNotEqualTo(NOT_FOUND);
  }

  private static Http.RequestBuilder request(String method, String path) {
    return fakeRequestBuilder().location(method, path).header(Http.HeaderNames.HOST, "localhost");
  }
}
