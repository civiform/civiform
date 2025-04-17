package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.route;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import com.typesafe.config.Config;
import controllers.applicant.ApplicantRoutes;
import java.util.Optional;
import org.junit.Test;
import org.pac4j.core.context.HttpConstants;
import play.i18n.MessagesApi;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import repository.HealthCheckRepository;
import repository.ResetPostgres;
import support.CfTestHelpers;
import support.CfTestHelpers.ResultWithFinalRequestUri;

public class HomeControllerTest extends ResetPostgres {
  @Test
  public void testSecurePage() {
    // This test accesses a resource that is protected by a @Secure annotation.
    // The test ensures that the requester gets a pac4j profile so that it can access
    // the resource.
    Http.RequestBuilder request =
        fakeRequestBuilder()
            .call(routes.HomeController.securePlayIndex())
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    ResultWithFinalRequestUri resultWithFinalRequestUri =
        CfTestHelpers.doRequestWithInternalRedirects(app, request);

    assertThat(resultWithFinalRequestUri.getResult().status()).isEqualTo(HttpConstants.OK);
    assertThat(resultWithFinalRequestUri.getFinalRequestUri())
        .isEqualTo(routes.HomeController.securePlayIndex().url());
  }

  @Test
  public void testFavicon() {
    Http.RequestBuilder request =
        fakeRequestBuilder()
            .call(routes.HomeController.favicon())
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    ResultWithFinalRequestUri resultWithFinalRequestUri =
        CfTestHelpers.doRequestWithInternalRedirects(app, request);
    Result result = resultWithFinalRequestUri.getResult();
    assertThat(result.redirectLocation()).isNotEmpty();
    assertThat(result.redirectLocation().get()).contains("civiform.us/favicon");
  }

  @Test
  public void testPlayIndex() {
    Http.RequestBuilder request =
        fakeRequestBuilder()
            .call(routes.HomeController.playIndex())
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    Result result = route(app, request);
    assertThat(result.status()).isEqualTo(200);
  }

  @Test
  public void testPlayIndexFail() {
    HealthCheckRepository healthCheckRepository = mock(HealthCheckRepository.class);
    when(healthCheckRepository.checkDBHealth()).thenReturn(Optional.empty());
    HomeController controller =
        new HomeController(
            instanceOf(Config.class),
            instanceOf(ProfileUtils.class),
            instanceOf(MessagesApi.class),
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(LanguageUtils.class),
            new ApplicantRoutes(),
            healthCheckRepository);
    Result result = controller.playIndex();
    assertThat(result.status()).isEqualTo(400);
  }
}
