package auth.oidc.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;

import auth.ProfileFactory;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.play.PlayWebContext;
import play.api.test.Helpers;
import play.mvc.Http.Request;
import repository.ResetPostgres;
import repository.UserRepository;
import support.CfTestHelpers;

@RunWith(JUnitParamsRunner.class)
public class LoginGovProviderTest extends ResetPostgres {
  private LoginGovProvider loginGovProvider;
  private ProfileFactory profileFactory;
  private static UserRepository userRepository;
  private static String DISCOVERY_URI = "http://oidc:3380/.well-known/openid-configuration";
  private static String BASE_URL = String.format("http://localhost:%d", Helpers.testServerPort());

  private static final SessionStore mockSessionStore = Mockito.mock(SessionStore.class);
  private static final Request requestMock = fakeRequest().remoteAddress("1.1.1.1").build();
  private static final PlayWebContext webContext = new PlayWebContext(requestMock);

  @Before
  public void setup() {
    userRepository = instanceOf(UserRepository.class);
    profileFactory = instanceOf(ProfileFactory.class);
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.of(
                "login_gov.client_id",
                "login:gov:client",
                "login_gov.discovery_uri",
                DISCOVERY_URI,
                "login_gov.acr_value",
                "http://acr.test",
                "base_url",
                BASE_URL));

    // Just need some complete adaptor to access methods.
    loginGovProvider =
        new LoginGovProvider(
            config, profileFactory, CfTestHelpers.userRepositoryProvider(userRepository));
  }

  @Test
  public void Test_getConfigurationValues() {
    OidcClient client = loginGovProvider.get();
    OidcConfiguration client_config = client.getConfiguration();
    ProfileCreator adaptor = loginGovProvider.getProfileAdapter(client_config, client);

    String clientId = loginGovProvider.getClientID();
    assertThat(clientId).isEqualTo("login:gov:client");

    String discoveryUri = loginGovProvider.getDiscoveryURI();
    assertThat(discoveryUri).isEqualTo(DISCOVERY_URI);

    String responseType = loginGovProvider.getResponseType();
    assertThat(responseType).isEqualTo("code");

    String responseMode = loginGovProvider.getResponseMode();
    assertThat(responseMode).isEqualTo("query");

    String callbackUrl = client.getCallbackUrl();
    assertThat(callbackUrl).isEqualTo(BASE_URL + "/callback");

    assertThat(adaptor.getClass()).isEqualTo(GenericOidcProfileAdapter.class);

    String provider = loginGovProvider.getProviderName().orElse("");
    assertThat(provider).isEqualTo("LoginGov");

    String scope = loginGovProvider.getScopesAttribute();
    assertThat(scope).isEqualTo("openid profile email");

    String acr = loginGovProvider.getACRValue();
    assertThat(acr).isEqualTo("http://acr.test");
  }

  @Test
  public void Test_redirectURI() {
    OidcClient client = loginGovProvider.get();

    var redirectAction = client.getRedirectionAction(webContext, mockSessionStore);
    assertThat(redirectAction).isNotEmpty();
    assertThat(redirectAction.toString()).contains("scope=openid+profile+email");
    assertThat(redirectAction.toString()).contains("response_type=code");
    assertThat(redirectAction.toString()).contains("acr_values=http%3A%2F%2Facr.test");
    assertThat(redirectAction.toString()).contains("state=");
    assertThat(redirectAction.toString()).contains("code_challenge_method=S256");
    assertThat(redirectAction.toString()).contains("prompt=select_account");
    assertThat(redirectAction.toString()).contains("nonce=");
    assertThat(redirectAction.toString()).contains("client_id=login%3Agov%3Aclient");
    assertThat(redirectAction.toString()).contains("code_challenge=");
  }
}
