package auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;
import static play.test.Helpers.fakeRequest;

import auth.CiviFormProfileData;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provider;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.PlainJWT;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import filters.SessionIdFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import models.Account;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.play.PlayWebContext;
import repository.AccountRepository;
import repository.ResetPostgres;
import support.CfTestHelpers;

public class CiviformOidcLogoutActionBuilderTest extends ResetPostgres {
  private static final String oidcHost = "dev-oidc";
  private static final int oidcPort = 3390;
  private static final String clientId = "test-client-id";
  private static final String targetUrl = "http://example.com/target";
  private static final String sessionId = "test-session-id";
  private static final long accountId = 1L;

  private IdTokensFactory idTokensFactory;
  private AccountRepository accountRepository;
  private OidcConfiguration oidcConfig;
  private CiviFormProfileData civiFormProfileData;
  private String idToken;

  @Before
  public void setup() {
    accountRepository = Mockito.mock(AccountRepository.class);
    idTokensFactory = instanceOf(IdTokensFactory.class);
    oidcConfig = CfTestHelpers.getOidcConfiguration(oidcHost, oidcPort);
    civiFormProfileData = new CiviFormProfileData(accountId);

    // Build and serialize a minimal JWT as an id token.
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().build();
    JWT jwt = new PlainJWT(claimsSet);
    idToken = jwt.serialize();
  }

  private Optional<String> getRedirectLocation(RedirectionAction action) {
    // Unfortunately, the RedirectionAction has almost no useful accessor methods, so we have to use
    // the `toString()` representation, which looks like:
    //
    // #FoundAction# | code: 302 | location: http://dev-oidc:3390/session/end?... |
    Pattern pattern = Pattern.compile("location: (https?://[^ ]+)");
    Matcher matcher = pattern.matcher(action.toString());
    if (matcher.find()) {
      return Optional.of(matcher.group(1));
    } else {
      return Optional.empty();
    }
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

  WebContext getWebContext(Optional<String> sessionId) {
    if (sessionId.isPresent()) {
      return new PlayWebContext(
          fakeRequest().session(SessionIdFilter.SESSION_ID, sessionId.get()).build());
    } else {
      return new PlayWebContext(fakeRequest().build());
    }
  }

  @Test
  public void testBuilderWithSessionId() throws URISyntaxException {
    Config civiformConfig = ConfigFactory.parseMap(ImmutableMap.of());

    Account account = new Account();
    SerializedIdTokens serializedIdTokens =
        new SerializedIdTokens(ImmutableMap.of(sessionId, idToken));
    account.setSerializedIdTokens(serializedIdTokens);
    when(accountRepository.lookupAccount(accountId)).thenReturn(Optional.of(account));
    Provider<AccountRepository> accountRepositoryProvider = () -> accountRepository;

    CiviformOidcLogoutActionBuilder builder =
        new CiviformOidcLogoutActionBuilder(
            civiformConfig, oidcConfig, clientId, accountRepositoryProvider, idTokensFactory);

    WebContext context = getWebContext(Optional.of(sessionId));
    SessionStore sessionStore = Mockito.mock(SessionStore.class);

    Optional<RedirectionAction> logoutAction =
        builder.getLogoutAction(context, sessionStore, civiFormProfileData, targetUrl);

    assertThat(logoutAction).isNotEmpty();
    assertThat(logoutAction.get().getCode()).isEqualTo(302);

    Optional<String> location = getRedirectLocation(logoutAction.get());
    assertThat(location).isNotEmpty();
    URI locationUri = new URI(location.get());
    assertThat(locationUri.getHost()).isEqualTo(oidcHost);
    assertThat(locationUri.getPort()).isEqualTo(oidcPort);
    assertThat(locationUri.getPath()).isEqualTo("/session/end");

    assertThat(queryParamValue(locationUri, "client_id")).hasValue(clientId);
    assertThat(queryParamValue(locationUri, "post_logout_redirect_uri")).hasValue(targetUrl);

    Optional<String> serializedToken = queryParamValue(locationUri, "id_token_hint");
    assertThat(serializedToken).isNotEmpty();
    assertThatCode(() -> JWTParser.parse(serializedToken.get())).doesNotThrowAnyException();
  }

  @Test
  public void testBuilderWithoutSessionId() throws URISyntaxException {
    Config civiformConfig = ConfigFactory.parseMap(ImmutableMap.of());

    Account account = new Account();
    SerializedIdTokens serializedIdTokens =
        new SerializedIdTokens(ImmutableMap.of(sessionId, idToken));
    account.setSerializedIdTokens(serializedIdTokens);
    when(accountRepository.lookupAccount(accountId)).thenReturn(Optional.of(account));
    Provider<AccountRepository> accountRepositoryProvider = () -> accountRepository;

    CiviformOidcLogoutActionBuilder builder =
        new CiviformOidcLogoutActionBuilder(
            civiformConfig, oidcConfig, clientId, accountRepositoryProvider, idTokensFactory);

    // This context does not contain a session id.
    WebContext context = getWebContext(Optional.empty());
    SessionStore sessionStore = Mockito.mock(SessionStore.class);

    Optional<RedirectionAction> logoutAction =
        builder.getLogoutAction(context, sessionStore, civiFormProfileData, targetUrl);

    assertThat(logoutAction).isNotEmpty();
    assertThat(logoutAction.get().getCode()).isEqualTo(302);

    Optional<String> location = getRedirectLocation(logoutAction.get());
    assertThat(location).isNotEmpty();
    URI locationUri = new URI(location.get());
    assertThat(locationUri.getHost()).isEqualTo(oidcHost);
    assertThat(locationUri.getPort()).isEqualTo(oidcPort);
    assertThat(locationUri.getPath()).isEqualTo("/session/end");

    assertThat(queryParamValue(locationUri, "client_id")).hasValue(clientId);
    assertThat(queryParamValue(locationUri, "post_logout_redirect_uri")).hasValue(targetUrl);

    // Since there is no session id, the id_token_hint parameter should not be set.
    Optional<String> serializedToken = queryParamValue(locationUri, "id_token_hint");
    assertThat(serializedToken).isEmpty();
  }

  @Test
  public void testBuilderWithAlternateTargetUrlParameterName() throws URISyntaxException {
    // By default, the `post_logout_redirect_url` is parameter is set. In this case, we want to use
    // a custom parameter name.
    Config civiformConfig =
        ConfigFactory.parseMap(
            ImmutableMap.of("auth.oidc_post_logout_param", "custom_target_url_parameter_name"));

    Account account = new Account();
    SerializedIdTokens serializedIdTokens =
        new SerializedIdTokens(ImmutableMap.of(sessionId, idToken));
    account.setSerializedIdTokens(serializedIdTokens);
    when(accountRepository.lookupAccount(accountId)).thenReturn(Optional.of(account));
    Provider<AccountRepository> accountRepositoryProvider = () -> accountRepository;

    CiviformOidcLogoutActionBuilder builder =
        new CiviformOidcLogoutActionBuilder(
            civiformConfig, oidcConfig, clientId, accountRepositoryProvider, idTokensFactory);

    WebContext context = getWebContext(Optional.of(sessionId));
    SessionStore sessionStore = Mockito.mock(SessionStore.class);

    Optional<RedirectionAction> logoutAction =
        builder.getLogoutAction(context, sessionStore, civiFormProfileData, targetUrl);

    assertThat(logoutAction).isNotEmpty();
    assertThat(logoutAction.get().getCode()).isEqualTo(302);

    Optional<String> location = getRedirectLocation(logoutAction.get());
    assertThat(location).isNotEmpty();
    URI locationUri = new URI(location.get());
    assertThat(locationUri.getHost()).isEqualTo(oidcHost);
    assertThat(locationUri.getPort()).isEqualTo(oidcPort);
    assertThat(locationUri.getPath()).isEqualTo("/session/end");

    assertThat(queryParamValue(locationUri, "client_id")).hasValue(clientId);
    // Check the value of the custom parameter.
    assertThat(queryParamValue(locationUri, "custom_target_url_parameter_name"))
        .hasValue(targetUrl);

    Optional<String> serializedToken = queryParamValue(locationUri, "id_token_hint");
    assertThat(serializedToken).isNotEmpty();
    assertThatCode(() -> JWTParser.parse(serializedToken.get())).doesNotThrowAnyException();
  }
}
