package auth.oidc.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;

import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.oidc.IdTokensFactory;
import auth.oidc.OidcClientProviderParams;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.net.URI;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.play.PlayWebContext;
import play.api.test.Helpers;
import play.mvc.Http.Request;
import repository.AccountRepository;
import repository.ResetPostgres;
import support.CfTestHelpers;

@RunWith(JUnitParamsRunner.class)
public class LoginGovClientProviderTest extends ResetPostgres {
  private LoginGovClientProvider loginGovProvider;
  private static final String DISCOVERY_URI =
      "http://dev-oidc:3390/.well-known/openid-configuration";
  private static final String BASE_URL =
      String.format("http://localhost:%d", Helpers.testServerPort());
  private static final String CLIENT_ID = "login:gov:client";

  private static final SessionStore mockSessionStore = Mockito.mock(SessionStore.class);
  private static final Request requestMock = fakeRequest().remoteAddress("1.1.1.1").build();
  private static final PlayWebContext webContext = new PlayWebContext(requestMock);

  @Before
  public void setup() {
    AccountRepository accountRepository = instanceOf(AccountRepository.class);
    ProfileFactory profileFactory = instanceOf(ProfileFactory.class);
    IdTokensFactory idTokensFactory = instanceOf(IdTokensFactory.class);
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.<String, String>builder()
                .put("login_gov.client_id", CLIENT_ID)
                .put("login_gov.discovery_uri", DISCOVERY_URI)
                .put("login_gov.acr_value", "http://acr.test")
                .put("base_url", BASE_URL)
                .build());

    // Just need some complete adaptor to access methods.
    loginGovProvider =
        new LoginGovClientProvider(
            OidcClientProviderParams.create(
                config,
                profileFactory,
                idTokensFactory,
                CfTestHelpers.userRepositoryProvider(accountRepository)));
  }

  @Test
  public void testGetConfigurationValues() {
    OidcClient client = loginGovProvider.get();
    OidcConfiguration client_config = client.getConfiguration();
    ProfileCreator adaptor = loginGovProvider.getProfileCreator(client_config, client);

    String clientId = loginGovProvider.getClientID();
    assertThat(clientId).isEqualTo(CLIENT_ID);

    String discoveryUri = loginGovProvider.getDiscoveryURI();
    assertThat(discoveryUri).isEqualTo(DISCOVERY_URI);

    String responseType = loginGovProvider.getResponseType();
    assertThat(responseType).isEqualTo("code");

    String responseMode = loginGovProvider.getResponseMode();
    assertThat(responseMode).isEqualTo("query");

    String callbackUrl = client.getCallbackUrl();
    assertThat(callbackUrl).isEqualTo(BASE_URL + "/callback");

    assertThat(adaptor.getClass()).isEqualTo(GenericApplicantProfileCreator.class);

    String provider = loginGovProvider.getProviderName().orElse("");
    assertThat(provider).isEqualTo("LoginGov");

    String scope = loginGovProvider.getScopesAttribute();
    assertThat(scope).isEqualTo("openid profile email");

    String acr = loginGovProvider.getACRValue();
    assertThat(acr).isEqualTo("http://acr.test");
  }

  @Test
  public void testRedirectURI() throws Exception {
    OidcClient client = loginGovProvider.get();

    var redirectAction = client.getRedirectionAction(webContext, mockSessionStore);
    assertThat(redirectAction).containsInstanceOf(FoundAction.class);
    var redirectUri = new URI(((FoundAction) redirectAction.get()).getLocation());
    assertThat(redirectUri)
        .hasParameter("scope", "openid profile email")
        .hasParameter("response_type", "code")
        .hasParameter("acr_values", "http://acr.test")
        .hasParameter("state")
        .hasParameter("code_challenge_method", "S256")
        .hasParameter("prompt", "select_account")
        .hasParameter("nonce")
        .hasParameter("client_id", CLIENT_ID)
        .hasParameter("code_challenge");
  }

  @Test
  public void testLogout() throws Exception {
    OidcClient client = loginGovProvider.get();
    String afterLogoutUri = "https://civiform.dev";
    var logoutAction =
        client.getLogoutAction(
            webContext, mockSessionStore, new CiviFormProfileData(1L), afterLogoutUri);
    assertThat(logoutAction).containsInstanceOf(FoundAction.class);
    var logoutUri = new URI(((FoundAction) logoutAction.get()).getLocation());
    assertThat(logoutUri)
        // host and path come from DISCOVERY_URI
        .hasHost("dev-oidc")
        .hasPath("/session/end")
        .hasParameter("state")
        .hasParameter("client_id", CLIENT_ID)
        .hasParameter("post_logout_redirect_uri", afterLogoutUri);
  }
}
