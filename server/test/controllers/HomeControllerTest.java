package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.route;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import controllers.applicant.ApplicantRoutes;
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
  public void testFaviconWhenSet() {
    Config config =
        ConfigFactory.parseMap(ImmutableMap.of("favicon_url", "https://civiform.us/favicon.png"));

    HomeController controller =
        new HomeController(
            config,
            instanceOf(ProfileUtils.class),
            instanceOf(MessagesApi.class),
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(LanguageUtils.class),
            new ApplicantRoutes(),
            instanceOf(HealthCheckRepository.class));
    Result result = controller.favicon();
    assertThat(result.redirectLocation()).isNotEmpty();
    assertThat(result.redirectLocation().get()).contains("civiform.us/favicon");
  }

  @Test
  public void testFaviconWhenNotSet() {
    Config config = ConfigFactory.parseMap(ImmutableMap.of("favicon_url", ""));

    HomeController controller =
        new HomeController(
            config,
            instanceOf(ProfileUtils.class),
            instanceOf(MessagesApi.class),
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(LanguageUtils.class),
            new ApplicantRoutes(),
            instanceOf(HealthCheckRepository.class));
    Result result = controller.favicon();
    assertThat(result.status()).isEqualTo(404);
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
    when(healthCheckRepository.isDBReachable()).thenReturn(false);
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
