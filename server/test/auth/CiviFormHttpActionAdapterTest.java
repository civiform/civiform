package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;

import org.junit.Test;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.exception.http.StatusAction;
import org.pac4j.play.PlayWebContext;
import play.mvc.Result;

public class CiviFormHttpActionAdapterTest {

  // An UNAUTHORIZED response to an API route should remain UNAUTHORIZED.
  @Test
  public void testApiUnauthorizedAction() {
    CiviFormHttpActionAdapter adapter = new CiviFormHttpActionAdapter();
    PlayWebContext context = new PlayWebContext(fakeRequest("GET", "/api/v1/checkAuth").build());

    Result result = adapter.adapt(new StatusAction(HttpConstants.UNAUTHORIZED), context);

    assertThat(result.status()).isEqualTo(HttpConstants.UNAUTHORIZED);
  }

  // A FORBIDDEN response to a non-API route should redirect to the home page.
  @Test
  public void testNonApiForbiddenAction() {
    CiviFormHttpActionAdapter adapter = new CiviFormHttpActionAdapter();
    PlayWebContext context = new PlayWebContext(fakeRequest("GET", "/non-api").build());

    Result result = adapter.adapt(new StatusAction(HttpConstants.FORBIDDEN), context);

    assertThat(result.status()).isEqualTo(HttpConstants.SEE_OTHER);
    assertThat(result.redirectLocation()).isNotEmpty();
    assertThat(result.redirectLocation().get()).isEqualTo("/");
  }

  // An UNAUTHORIZED response to a non-API route should redirect to the home page.
  @Test
  public void testNonApiUnauthorizedAction() {
    CiviFormHttpActionAdapter adapter = new CiviFormHttpActionAdapter();
    PlayWebContext context = new PlayWebContext(fakeRequest("GET", "/non-api").build());

    Result result = adapter.adapt(new StatusAction(HttpConstants.UNAUTHORIZED), context);

    assertThat(result.status()).isEqualTo(HttpConstants.SEE_OTHER);
    assertThat(result.redirectLocation()).isNotEmpty();
    assertThat(result.redirectLocation().get()).isEqualTo("/");
  }

  // An OK response should just ... be OK.
  @Test
  public void testOkAction() {
    CiviFormHttpActionAdapter adapter = new CiviFormHttpActionAdapter();
    PlayWebContext context = new PlayWebContext(fakeRequest("GET", "/arbitrary").build());

    Result result = adapter.adapt(new StatusAction(HttpConstants.OK), context);

    assertThat(result.status()).isEqualTo(HttpConstants.OK);
  }
}
