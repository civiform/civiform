package auth.oidc;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfileData;
import com.google.common.collect.ImmutableMap;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import com.typesafe.config.Config;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.core.util.HttpActionHelper;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.logout.OidcLogoutActionBuilder;

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
 * <p>If the oidc_logout_client_id_param config variable is set, also adds the client_id to the
 * logout request.
 */
public final class CiviformOidcLogoutActionBuilder extends OidcLogoutActionBuilder {

  private final Optional<String> postLogoutRedirectParam;
  private final ImmutableMap<String, String> extraParams;

  public CiviformOidcLogoutActionBuilder(
      Config civiformConfiguration, final OidcConfiguration oidcConfiguration, String clientID) {
    super(oidcConfiguration);
    checkNotNull(civiformConfiguration);
    this.postLogoutRedirectParam =
        getConfigurationValue(civiformConfiguration, "auth.oidc_post_logout_param");
    Optional<String> clientIdParam =
        getConfigurationValue(civiformConfiguration, "auth.oidc_logout_client_id_param");

    if (clientIdParam.isPresent()) {
      this.extraParams = ImmutableMap.of(clientIdParam.get(), clientID);
    } else {
      this.extraParams = ImmutableMap.of();
    }
  }

  /** Helper function for retriving values from the application.conf, */
  protected static final Optional<String> getConfigurationValue(
      Config civiformConfiguration, String name) {
    if (civiformConfiguration.hasPath(name)) {
      return Optional.ofNullable(civiformConfiguration.getString(name));
    }
    return Optional.empty();
  }

  /**
   * Override the parent's getLogoutAction, since it check that the profile is an instance of
   * OidcProfile, and uses fields we don't have access to. Generally keeps the same basic logic.
   *
   * <p>Also use the custom CustomOidcLogoutRequest to build the URL.
   */
  @Override
  public Optional<RedirectionAction> getLogoutAction(
      final WebContext context,
      final SessionStore sessionStore,
      final UserProfile currentProfile,
      final String targetUrl) {
    final String logoutUrl = configuration.findLogoutUrl();
    if (CommonHelper.isNotBlank(logoutUrl) && currentProfile instanceof CiviFormProfileData) {
      try {
        final URI endSessionEndpoint = new URI(logoutUrl);
        LogoutRequest logoutRequest =
            new CustomOidcLogoutRequest(
                endSessionEndpoint,
                postLogoutRedirectParam.orElse(null),
                new URI(targetUrl),
                extraParams);

        return Optional.of(
            HttpActionHelper.buildRedirectUrlAction(context, logoutRequest.toURI().toString()));
      } catch (final URISyntaxException e) {
        throw new TechnicalException(e);
      }
    }

    return Optional.empty();
  }
}
