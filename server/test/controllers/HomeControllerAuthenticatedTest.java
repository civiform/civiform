package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Locale;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.direct.AnonymousClient;
import org.pac4j.core.client.finder.ClientFinder;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.engine.DefaultSecurityLogic;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import views.LoginForm;

public class HomeControllerAuthenticatedTest extends WithMockedProfiles {
  @Before
  public void setUp() {
    resetDatabase();
    // Get the config, and hack it so that all requests appear authorized.
    Config config = instanceOf(Config.class);
    AnonymousClient client = AnonymousClient.INSTANCE;
    config.setClients(new Clients(client));

    // The SecurityLogic wants to use some smarts to figure out which client to use, but
    // those smarts are never going to pick this client (since none of the endpoints are
    // configured to use it), so we implement an anonymous client finder which always returns
    // our client.
    DefaultSecurityLogic securityLogic = new DefaultSecurityLogic();
    securityLogic.setClientFinder(
        new ClientFinder() {
          @Override
          public List<Client> find(Clients clients, WebContext context, String clientNames) {
            return ImmutableList.of(client);
          }
        });
    config.setSecurityLogic(securityLogic);
  }

  @Test
  public void testAuthenticatedSecurePage() {
    Http.RequestBuilder request =
        fakeRequest(routes.HomeController.securePlayIndex())
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    Result result = route(app, request);
    assertThat(result.status()).isEqualTo(HttpConstants.OK);
  }

  @Test
  public void testLanguageSelectorShown() {
    Applicant applicant = createApplicantWithMockedProfile();
    HomeController controller = instanceOf(HomeController.class);
    Result result =
        controller.index(addCSRFToken(fakeRequest()).build()).toCompletableFuture().join();
    ;
    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantInformationController.edit(applicant.id).url());
  }

  @Test
  public void testLanguageSelectorNotShownOneLanguage() {
    Applicant applicant = createApplicantWithMockedProfile();
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));
    LanguageUtils languageUtils = new LanguageUtils(instanceOf(UserRepository.class), mockLangs);

    HomeController controller =
        new HomeController(
            instanceOf(LoginForm.class),
            instanceOf(ProfileUtils.class),
            instanceOf(MessagesApi.class),
            instanceOf(HttpExecutionContext.class),
            languageUtils);
    Result result =
        controller.index(addCSRFToken(fakeRequest()).build()).toCompletableFuture().join();
    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramsController.index(applicant.id).url());
  }

  @Test
  public void testLanguageSelectorNotShownOneLanguage() {
    Applicant applicant = createApplicantWithMockedProfile();
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));
    LanguageUtils languageUtils = new LanguageUtils(instanceOf(UserRepository.class), mockLangs);

    HomeController controller =
        new HomeController(
            instanceOf(LoginForm.class),
            instanceOf(ProfileUtils.class),
            instanceOf(MessagesApi.class),
            instanceOf(HttpExecutionContext.class),
            languageUtils);
    Result result =
        controller.index(addCSRFToken(fakeRequest()).build()).toCompletableFuture().join();
    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramsController.index(applicant.id).url());
  }

  @Test
  public void testLanguageSelectorNotSupportedLanguage() {
    Applicant applicant = createApplicantWithMockedProfile();
    applicant.getApplicantData().setPreferredLocale(Locale.CHINESE);
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));
    LanguageUtils languageUtils = new LanguageUtils(instanceOf(UserRepository.class), mockLangs);

    HomeController controller =
        new HomeController(
            instanceOf(LoginForm.class),
            instanceOf(ProfileUtils.class),
            instanceOf(MessagesApi.class),
            instanceOf(HttpExecutionContext.class),
            languageUtils);
    Result result =
        controller.index(addCSRFToken(fakeRequest()).build()).toCompletableFuture().join();
    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramsController.index(applicant.id).url());
  }
}
