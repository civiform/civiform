package auth.oidc;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfileData;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import com.typesafe.config.Config;
import filters.SessionIdFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Optional;
import javax.inject.Provider;
import models.Account;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.core.util.HttpActionHelper;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.logout.OidcLogoutActionBuilder;
import org.pac4j.play.PlayWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.AccountRepository;

/**
 * Custom OidcLogoutActionBuilder for CiviFormProfileData (since it extends CommonProfile, not
 * OidcProfile). Also allows for divergence from the [oidc
 * spec](https://openid.net/specs/openid-connect-rpinitiated-1_0.html) if your provider requires it
 * (e.g. Auth0).
 *
 * <p>Does not provide the recommended id_token_hint, since these are not stored by the civiform
 * profile.
 *
 * <p>Uses the post_logout_redirect_uri parameter by default, but allows overriding to a different
 * value using the auth.oidc_post_logout_param config variable
 *
 * <p>Always adds the client_id to the logout request.
 */
public final class CiviformOidcLogoutActionBuilder extends OidcLogoutActionBuilder {
  private static Logger logger = LoggerFactory.getLogger(CiviformOidcLogoutActionBuilder.class);

  private String postLogoutRedirectParam;
  private final String clientId;
  private Provider<AccountRepository> accountRepositoryProvider;
  private final IdTokensFactory idTokensFactory;

  public CiviformOidcLogoutActionBuilder(
      Config civiformConfiguration,
      OidcConfiguration oidcConfiguration,
      String clientId,
      Provider<AccountRepository> accountRepositoryProvider,
      IdTokensFactory idTokensFactory) {
    super(oidcConfiguration);
    checkNotNull(civiformConfiguration);
    // Use `post_logout_redirect_uri` by default according OIDC spec.
    this.postLogoutRedirectParam =
        getConfigurationValue(civiformConfiguration, "auth.oidc_post_logout_param")
            .orElse("post_logout_redirect_uri");

    this.clientId = clientId;
    this.accountRepositoryProvider = accountRepositoryProvider;
    this.idTokensFactory = checkNotNull(idTokensFactory);
  }

  /** Helper function for retriving values from the application.conf, */
  private static Optional<String> getConfigurationValue(Config civiformConfiguration, String name) {
    if (civiformConfiguration.hasPath(name)) {
      return Optional.ofNullable(civiformConfiguration.getString(name));
    }
    return Optional.empty();
  }

  /**
   * Sets param that contains uri that user will be redirected to after they are logged out from the
   * auth provider. In OIDC spec it should be `post_logout_redirect_uri` but some providers use
   * different value.
   */
  public CiviformOidcLogoutActionBuilder setPostLogoutRedirectParam(String param) {
    this.postLogoutRedirectParam = param;
    return this;
  }

  private JWT getIdTokenForAccount(long accountId, WebContext context) {
    PlayWebContext playWebContext = (PlayWebContext) context;
    Optional<String> sessionId = playWebContext.getNativeSession().get(SessionIdFilter.SESSION_ID);
    logger.info("XXX CiviformOidcLogoutActionBuilder: sessionId = {}", sessionId);
    if (sessionId.isEmpty()) {
      // The session id is only populated if the feature flag is enabled.
      return null;
    }
    Optional<Account> account = accountRepositoryProvider.get().lookupAccount(accountId);
    logger.info("XXX CiviformOidcLogoutActionBuilder: account = {}", account);
    if (account.isEmpty()) {
      return null;
    }
    SerializedIdTokens serializedIdTokens = account.get().getSerializedIdTokens();
    IdTokens idTokens = idTokensFactory.create(serializedIdTokens);
    // When we build the logout action, we do not remove the id token. We leave it in place in case
    // of transient logout failures. Expired tokens are purged at login time instead.
    Optional<String> idToken = idTokens.getIdToken(sessionId.get());
    logger.info("XXX CiviformOidcLogoutActionBuilder: idToken = {}", idToken);
    if (idToken.isEmpty()) {
      return null;
    }
    try {
      return JWTParser.parse(idToken.get());
    } catch (ParseException e) {
      return null;
    }
  }

  /**
   * Override the parent's getLogoutAction, since it checks that the profile is an instance of
   * OidcProfile, and uses fields we don't have access to. Generally keeps the same basic logic.
   *
   * <p>Also use the custom CustomOidcLogoutRequest to build the URL.
   */
  @Override
  public Optional<RedirectionAction> getLogoutAction(
      WebContext context, SessionStore sessionStore, UserProfile currentProfile, String targetUrl) {
    String logoutUrl = configuration.findLogoutUrl();
    if (CommonHelper.isNotBlank(logoutUrl) && currentProfile instanceof CiviFormProfileData) {
      try {
        URI endSessionEndpoint = new URI(logoutUrl);

        // Optional state param for logout is only needed by certain OIDC providers, but we
        // always include it since it can help with cross-site forgery attacks.
        // OidcConfiguration comes with a default state generator.
        State state =
            new State(configuration.getStateGenerator().generateValue(context, sessionStore));

        long accountId = Long.parseLong(currentProfile.getId());
        JWT idToken = getIdTokenForAccount(accountId, context);

        LogoutRequest logoutRequest =
            new CustomOidcLogoutRequest(
                endSessionEndpoint,
                postLogoutRedirectParam,
                new URI(targetUrl),
                Optional.of(clientId),
                state,
                idToken);

        return Optional.of(
            HttpActionHelper.buildRedirectUrlAction(context, logoutRequest.toURI().toString()));
      } catch (URISyntaxException e) {
        throw new TechnicalException(e);
      }
    }

    return Optional.empty();
  }
}
