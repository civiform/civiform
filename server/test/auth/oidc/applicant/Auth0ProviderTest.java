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
import org.pac4j.play.PlayWebContext;
import play.api.test.Helpers;
import play.mvc.Http.Request;
import repository.AccountRepository;
import repository.ResetPostgres;
import support.CfTestHelpers;

@RunWith(JUnitParamsRunner.class)
public class Auth0ProviderTest extends ResetPostgres {
  private Auth0ClientProvider auth0Provider;
  private static final String DISCOVERY_URI =
      "http://dev-oidc:3390/.well-known/openid-configuration";
  private static final String BASE_URL =
      String.format("http://localhost:%d", Helpers.testServerPort());
  private static final String CLIENT_ID = "someFakeClientId";

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
                .put("applicant_generic_oidc.client_id", CLIENT_ID)
                .put("applicant_generic_oidc.client_secret", "secret_here")
                .put("applicant_generic_oidc.response_mode", "form_post")
                .put("applicant_generic_oidc.response_type", "id_token")
                .put("applicant_generic_oidc.email_attribute", "email")
                .put("applicant_generic_oidc.discovery_uri", DISCOVERY_URI)
                .put("base_url", BASE_URL)
                .build());

    // Just need some complete adaptor to access methods.
    auth0Provider =
        new Auth0ClientProvider(
            OidcClientProviderParams.create(
                config,
                profileFactory,
                idTokensFactory,
                CfTestHelpers.userRepositoryProvider(accountRepository)));
  }

  @Test
  public void testProviderName() {
    assertThat(auth0Provider.getProviderName()).contains("Auth0");
  }

  @Test
  public void testLogout() throws Exception {
    String afterLogoutUri = "https://civiform.dev";
    var logoutAction =
        auth0Provider
            .get()
            .getLogoutAction(
                webContext, mockSessionStore, new CiviFormProfileData(1L), afterLogoutUri);
    assertThat(logoutAction).containsInstanceOf(FoundAction.class);
    var logoutUri = new URI(((FoundAction) logoutAction.get()).getLocation());
    assertThat(logoutUri)
        // host and path come from DISCOVERY_URI
        .hasHost("dev-oidc")
        .hasPath("/v2/logout")
        .hasParameter("client_id", CLIENT_ID)
        .hasParameter("returnTo", afterLogoutUri);
  }
}
