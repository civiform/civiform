package controllers;

import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormHttpActionAdapter;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.exception.http.RedirectionAction;
import play.mvc.Http;
import play.mvc.Result;

public class LoginControllerTest {

  private IndirectClient mockApplicantClient;
  private IndirectClient mockAdminClient;
  private CiviFormHttpActionAdapter mockHttpActionAdapter;
  private SessionStore mockSessionStore;
  private Config config;
  private LoginController controller;

  @Before
  public void setUp() {
    mockApplicantClient = mock(IndirectClient.class);
    mockAdminClient = mock(IndirectClient.class);
    mockHttpActionAdapter = mock(CiviFormHttpActionAdapter.class);
    mockSessionStore = mock(SessionStore.class);
    config = ConfigFactory.parseMap(ImmutableMap.of("applicant_register_uri", ""));

    controller =
        new LoginController(
            mockAdminClient, mockApplicantClient, mockHttpActionAdapter, mockSessionStore, config);
  }

  @Test
  public void applicantLogin_withNoRedirect_removesRedirectToSessionKey() {
    Http.Request request =
        fakeRequestBuilder().addSessionValue(REDIRECT_TO_SESSION_KEY, "/old/redirect").build();

    // Setup redirect action
    RedirectionAction redirectAction = new FoundAction("https://auth.example.com/login");
    when(mockApplicantClient.getRedirectionAction(any(CallContext.class)))
        .thenReturn(Optional.of(redirectAction));

    Result mockResult = play.mvc.Results.redirect("https://auth.example.com/login");
    when(mockHttpActionAdapter.adapt(any(), any())).thenReturn(mockResult);

    Result result = controller.applicantLogin(request, Optional.empty());

    // The result should have the redirectTo session key removed
    assertThat(result.session().get(REDIRECT_TO_SESSION_KEY)).isEmpty();
  }

  @Test
  public void applicantLogin_withRedirect_addsRedirectToSession() {
    Http.Request request = fakeRequestBuilder().build();
    String expectedRedirect = "/programs/1";

    // Setup redirect action
    RedirectionAction redirectAction = new FoundAction("https://auth.example.com/login");
    when(mockApplicantClient.getRedirectionAction(any(CallContext.class)))
        .thenReturn(Optional.of(redirectAction));

    Result mockResult = play.mvc.Results.redirect("https://auth.example.com/login");
    when(mockHttpActionAdapter.adapt(any(), any())).thenReturn(mockResult);

    Result result = controller.applicantLogin(request, Optional.of(expectedRedirect));

    // The result should have the redirectTo session key set to the provided value
    assertThat(result.session().get(REDIRECT_TO_SESSION_KEY)).isPresent();
    assertThat(result.session().get(REDIRECT_TO_SESSION_KEY).get()).isEqualTo(expectedRedirect);
  }

  @Test
  public void applicantLogin_withRedirect_replacesExistingRedirectToSession() {
    Http.Request request =
        fakeRequestBuilder().addSessionValue(REDIRECT_TO_SESSION_KEY, "/old/redirect").build();
    String newRedirect = "/programs/2";

    // Setup redirect action
    RedirectionAction redirectAction = new FoundAction("https://auth.example.com/login");
    when(mockApplicantClient.getRedirectionAction(any(CallContext.class)))
        .thenReturn(Optional.of(redirectAction));

    Result mockResult = play.mvc.Results.redirect("https://auth.example.com/login");
    when(mockHttpActionAdapter.adapt(any(), any())).thenReturn(mockResult);

    Result result = controller.applicantLogin(request, Optional.of(newRedirect));

    // The result should have the new redirectTo value, replacing the old one
    assertThat(result.session().get(REDIRECT_TO_SESSION_KEY)).isPresent();
    assertThat(result.session().get(REDIRECT_TO_SESSION_KEY).get()).isEqualTo(newRedirect);
  }

  @Test
  public void applicantLogin_withNoClient_returnsBadRequest() {
    LoginController controllerWithNoClient =
        new LoginController(mockAdminClient, null, mockHttpActionAdapter, mockSessionStore, config);

    Http.Request request = fakeRequestBuilder().build();

    Result result = controllerWithNoClient.applicantLogin(request, Optional.empty());

    assertThat(result.status()).isEqualTo(Http.Status.BAD_REQUEST);
  }

  @Test
  public void adminLogin_withNoClient_returnsBadRequest() {
    LoginController controllerWithNoClient =
        new LoginController(
            null, mockApplicantClient, mockHttpActionAdapter, mockSessionStore, config);

    Http.Request request = fakeRequestBuilder().build();

    Result result = controllerWithNoClient.adminLogin(request);

    assertThat(result.status()).isEqualTo(Http.Status.BAD_REQUEST);
  }

  @Test
  public void adminLogin_withClient_redirectsToAuthProvider() {
    Http.Request request = fakeRequestBuilder().build();

    // Setup redirect action
    RedirectionAction redirectAction = new FoundAction("https://admin-auth.example.com/login");
    when(mockAdminClient.getRedirectionAction(any(CallContext.class)))
        .thenReturn(Optional.of(redirectAction));

    Result mockResult = play.mvc.Results.redirect("https://admin-auth.example.com/login");
    when(mockHttpActionAdapter.adapt(any(), any())).thenReturn(mockResult);

    Result result = controller.adminLogin(request);

    assertThat(result.status()).isEqualTo(Http.Status.SEE_OTHER);
  }

  @Test
  public void register_withNoRegisterUri_redirectsToApplicantLogin() {
    Http.Request request = fakeRequestBuilder().build();

    // Setup redirect action for applicant login
    RedirectionAction redirectAction = new FoundAction("https://auth.example.com/login");
    when(mockApplicantClient.getRedirectionAction(any(CallContext.class)))
        .thenReturn(Optional.of(redirectAction));

    Result mockResult = play.mvc.Results.redirect("https://auth.example.com/login");
    when(mockHttpActionAdapter.adapt(any(), any())).thenReturn(mockResult);

    Result result = controller.register(request);

    // With no register URI configured, should redirect to login
    assertThat(result.status()).isEqualTo(Http.Status.SEE_OTHER);
  }

  @Test
  public void register_withRegisterUri_redirectsToRegisterUrl() {
    Config configWithRegisterUri =
        ConfigFactory.parseMap(
            ImmutableMap.of("applicant_register_uri", "https://register.example.com/signup"));

    LoginController controllerWithRegisterUri =
        new LoginController(
            mockAdminClient,
            mockApplicantClient,
            mockHttpActionAdapter,
            mockSessionStore,
            configWithRegisterUri);

    Http.Request request = fakeRequestBuilder().build();

    Result result = controllerWithRegisterUri.register(request);

    assertThat(result.status()).isEqualTo(Http.Status.SEE_OTHER);
    assertThat(result.redirectLocation()).isPresent();
    assertThat(result.redirectLocation().get()).isEqualTo("https://register.example.com/signup");
    // Should set redirectTo in session to come back to applicant login after registration
    assertThat(result.session().get(REDIRECT_TO_SESSION_KEY)).isPresent();
  }

  @Test
  public void applicantLogin_whenNoRedirectAction_returnsBadRequest() {
    Http.Request request = fakeRequestBuilder().build();

    // Return empty optional for redirection action
    when(mockApplicantClient.getRedirectionAction(any(CallContext.class)))
        .thenReturn(Optional.empty());

    Result result = controller.applicantLogin(request, Optional.empty());

    assertThat(result.status()).isEqualTo(Http.Status.BAD_REQUEST);
  }
}
