package auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequest;

import auth.CiviFormProfileData;
import auth.IdentityProviderType;
import auth.ProfileFactory;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provider;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.PlainJWT;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import models.AccountModel;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.play.PlayWebContext;
import repository.AccountRepository;
import repository.ResetPostgres;
import support.CfTestHelpers;

@RunWith(MockitoJUnitRunner.class)
public class CiviformOidcLogoutActionBuilderTest extends ResetPostgres {
  private static final String oidcHost = "dev-oidc";
  private static final int oidcPort = 3390;
  private static final String clientId = "test-client-id";
  private static final String targetUrl = "http://example.com/target";
  private static final long accountId = 1L;

  private OidcConfiguration oidcConfig;
  private CiviFormProfileData civiFormProfileData;
  private String idToken;
  @Mock private AccountRepository accountRepository;
  @Mock private ProfileFactory profileFactory;
  @Mock private SessionStore sessionStore;

  @Before
  public void setup() {
    oidcConfig = CfTestHelpers.getOidcConfiguration(oidcHost, oidcPort);
    civiFormProfileData = new CiviFormProfileData(accountId, Clock.systemUTC());

    // Build and serialize a minimal JWT as an id token.
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().build();
    JWT jwt = new PlainJWT(claimsSet);
    idToken = jwt.serialize();
  }

  Optional<String> queryParamValue(URI uri, String paramName) {
    List<NameValuePair> queryParams = URLEncodedUtils.parse(uri, Charset.defaultCharset());
    // This assumes that parameter names are not repeated.
    for (NameValuePair nvp : queryParams) {
      if (nvp.getName().equals(paramName)) {
        return Optional.of(nvp.getValue());
      }
    }
    return Optional.empty();
  }

  WebContext getWebContext() {
    return new PlayWebContext(fakeRequest());
  }

  @Test
  public void testBuilder() throws URISyntaxException {
    Config civiformConfig = ConfigFactory.parseMap(ImmutableMap.of());

    AccountModel account = new AccountModel();
    IdTokens idTokens = new IdTokens(ImmutableMap.of(civiFormProfileData.getSessionId(), idToken));
    account.setIdTokens(idTokens);
    Provider<AccountRepository> accountRepositoryProvider = () -> accountRepository;

    OidcClientProviderParams params =
        OidcClientProviderParams.create(civiformConfig, profileFactory, accountRepositoryProvider);
    CiviformOidcLogoutActionBuilder builder =
        new CiviformOidcLogoutActionBuilder(
            oidcConfig, clientId, params, IdentityProviderType.APPLICANT_IDENTITY_PROVIDER);

    Optional<RedirectionAction> logoutAction =
        builder.getLogoutAction(
            new CallContext(getWebContext(), sessionStore), civiFormProfileData, targetUrl);

    assertThat(logoutAction).isNotEmpty();
    assertThat(logoutAction.get().getCode()).isEqualTo(302);

    String location = ((FoundAction) logoutAction.get()).getLocation();
    assertThat(location).isNotEmpty();
    URI locationUri = new URI(location);
    assertThat(locationUri.getHost()).isEqualTo(oidcHost);
    assertThat(locationUri.getPort()).isEqualTo(oidcPort);
    assertThat(locationUri.getPath()).isEqualTo("/session/end");

    assertThat(queryParamValue(locationUri, "client_id")).hasValue(clientId);
    assertThat(queryParamValue(locationUri, "post_logout_redirect_uri")).hasValue(targetUrl);
  }

  @Test
  public void testBuilderWithEnhancedLogoutEnabled() throws URISyntaxException {
    // Enable enhanced logout for admins.
    Config civiformConfig =
        ConfigFactory.parseMap(ImmutableMap.of("admin_oidc_enhanced_logout_enabled", "true"));

    // Set up an admin account. Associate the session ID with the ID token for logout.
    AccountModel account = new AccountModel();
    account.setGlobalAdmin(true);
    IdTokens idTokens = new IdTokens(ImmutableMap.of(civiFormProfileData.getSessionId(), idToken));
    account.setIdTokens(idTokens);
    when(accountRepository.lookupAccount(accountId)).thenReturn(Optional.of(account));
    Provider<AccountRepository> accountRepositoryProvider = () -> accountRepository;

    OidcClientProviderParams params =
        OidcClientProviderParams.create(civiformConfig, profileFactory, accountRepositoryProvider);
    CiviformOidcLogoutActionBuilder builder =
        new CiviformOidcLogoutActionBuilder(
            oidcConfig, clientId, params, IdentityProviderType.ADMIN_IDENTITY_PROVIDER);

    Optional<RedirectionAction> logoutAction =
        builder.getLogoutAction(
            new CallContext(getWebContext(), sessionStore), civiFormProfileData, targetUrl);

    assertThat(logoutAction).isNotEmpty();
    assertThat(logoutAction.get().getCode()).isEqualTo(302);

    String location = ((FoundAction) logoutAction.get()).getLocation();
    assertThat(location).isNotEmpty();
    URI locationUri = new URI(location);
    assertThat(locationUri.getHost()).isEqualTo(oidcHost);
    assertThat(locationUri.getPort()).isEqualTo(oidcPort);
    assertThat(locationUri.getPath()).isEqualTo("/session/end");

    assertThat(queryParamValue(locationUri, "client_id")).hasValue(clientId);
    assertThat(queryParamValue(locationUri, "post_logout_redirect_uri")).hasValue(targetUrl);

    // Additional validations for enhanced logout behavior.
    Optional<String> serializedToken = queryParamValue(locationUri, "id_token_hint");
    assertThat(serializedToken).isNotEmpty();
    assertThatCode(() -> JWTParser.parse(serializedToken.get())).doesNotThrowAnyException();
  }

  @Test
  public void testBuilderWithAlternateTargetUrlParameterName() throws URISyntaxException {
    // By default, the `post_logout_redirect_url` is parameter is set. In this case, we want to use
    // a custom parameter name.
    Config civiformConfig =
        ConfigFactory.parseMap(
            ImmutableMap.of("auth.oidc_post_logout_param", "custom_target_url_parameter_name"));

    AccountModel account = new AccountModel();
    IdTokens idTokens = new IdTokens(ImmutableMap.of(civiFormProfileData.getSessionId(), idToken));
    account.setIdTokens(idTokens);
    Provider<AccountRepository> accountRepositoryProvider = () -> accountRepository;

    OidcClientProviderParams params =
        OidcClientProviderParams.create(civiformConfig, profileFactory, accountRepositoryProvider);
    CiviformOidcLogoutActionBuilder builder =
        new CiviformOidcLogoutActionBuilder(
            oidcConfig, clientId, params, IdentityProviderType.APPLICANT_IDENTITY_PROVIDER);

    Optional<RedirectionAction> logoutAction =
        builder.getLogoutAction(
            new CallContext(getWebContext(), sessionStore), civiFormProfileData, targetUrl);

    assertThat(logoutAction).isNotEmpty();
    assertThat(logoutAction.get().getCode()).isEqualTo(302);

    String location = ((FoundAction) logoutAction.get()).getLocation();
    assertThat(location).isNotEmpty();
    URI locationUri = new URI(location);
    assertThat(locationUri.getHost()).isEqualTo(oidcHost);
    assertThat(locationUri.getPort()).isEqualTo(oidcPort);
    assertThat(locationUri.getPath()).isEqualTo("/session/end");

    assertThat(queryParamValue(locationUri, "client_id")).hasValue(clientId);
    // Check the value of the custom parameter.
    assertThat(queryParamValue(locationUri, "custom_target_url_parameter_name"))
        .hasValue(targetUrl);
  }
}
