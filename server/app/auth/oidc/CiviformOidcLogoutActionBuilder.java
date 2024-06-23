package auth.oidc;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfileData;
import auth.IdentityProviderType;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import com.typesafe.config.Config;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Optional;
import javax.inject.Provider;
import models.AccountModel;
import org.apache.commons.lang3.NotImplementedException;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.core.util.HttpActionHelper;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.logout.OidcLogoutActionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.AccountRepository;
import services.settings.SettingsManifest;

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

  private static final Logger LOGGER =
      LoggerFactory.getLogger(CiviformOidcLogoutActionBuilder.class);
  private String postLogoutRedirectParam;
  private final String clientId;
  private final Provider<AccountRepository> accountRepositoryProvider;
  private final IdentityProviderType identityProviderType;
  private final SettingsManifest settingsManifest;

  public CiviformOidcLogoutActionBuilder(
      OidcConfiguration oidcConfiguration,
      String clientId,
      OidcClientProviderParams params,
      IdentityProviderType identityProviderType) {
    super(oidcConfiguration);

    checkNotNull(params.configuration());
    // Use `post_logout_redirect_uri` by default according OIDC spec.
    this.postLogoutRedirectParam =
        getConfigurationValue(params.configuration(), "auth.oidc_post_logout_param")
            .orElse("post_logout_redirect_uri");

    this.clientId = clientId;
    this.accountRepositoryProvider = params.accountRepositoryProvider();
    this.identityProviderType = identityProviderType;
    this.settingsManifest = new SettingsManifest(params.configuration());
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

  private Optional<JWT> getIdTokenForAccount(
      long accountId, CallContext callContext, CiviFormProfileData profileData) {
    if (!enhancedLogoutEnabled()) {
      return Optional.empty();
    }

    String sessionId = profileData.getSessionId();

    Optional<AccountModel> account = accountRepositoryProvider.get().lookupAccount(accountId);
    if (account.isEmpty()) {
      return Optional.empty();
    }

    IdTokens idTokens = account.get().getIdTokens();

    // When we build the logout action, we do not remove the id token. We leave it in place in case
    // of transient logout failures. Expired tokens are purged at login time instead.
    Optional<String> idToken = idTokens.getIdToken(sessionId);
    if (idToken.isEmpty()) {
      return Optional.empty();
    }
    try {
      return Optional.of(JWTParser.parse(idToken.get()));
    } catch (ParseException e) {
      LOGGER.warn("Could not parse id token for account {}", accountId);
      return Optional.empty();
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
      CallContext callContext, UserProfile currentProfile, String targetUrl) {
    String logoutUrl = configuration.findLogoutUrl();
    if (CommonHelper.isNotBlank(logoutUrl) && currentProfile instanceof CiviFormProfileData) {
      try {
        URI endSessionEndpoint = new URI(logoutUrl);

        // Optional state param for logout is only needed by certain OIDC providers, but we
        // always include it since it can help with cross-site forgery attacks.
        // OidcConfiguration comes with a default state generator.
        State state = new State(configuration.getStateGenerator().generateValue(callContext));

        long accountId = Long.parseLong(currentProfile.getId());
        Optional<JWT> idToken =
            getIdTokenForAccount(accountId, callContext, (CiviFormProfileData) currentProfile);

        LogoutRequest logoutRequest =
            new CustomOidcLogoutRequest(
                endSessionEndpoint,
                postLogoutRedirectParam,
                new URI(targetUrl),
                Optional.of(clientId),
                state,
                idToken.orElse(null));

        return Optional.of(
            HttpActionHelper.buildRedirectUrlAction(
                callContext.webContext(), logoutRequest.toURI().toString()));
      } catch (URISyntaxException e) {
        throw new TechnicalException(e);
      }
    }

    return Optional.empty();
  }

  private boolean enhancedLogoutEnabled() {
    // Sigh. This would be much nicer with switch expressions (Java 12) and exhaustive switch (Java
    // 17).
    switch (identityProviderType) {
      case ADMIN_IDENTITY_PROVIDER:
        return settingsManifest.getAdminOidcEnhancedLogoutEnabled();
      case APPLICANT_IDENTITY_PROVIDER:
        return settingsManifest.getApplicantOidcEnhancedLogoutEnabled();
      default:
        throw new NotImplementedException(
            "Identity provider type not handled: " + identityProviderType);
    }
  }
}
