package auth.oidc;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfileData;
import com.nimbusds.oauth2.sdk.id.State;
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
 * <p>Always adds the client_id to the logout request.
 */
public final class CiviformOidcLogoutActionBuilder extends OidcLogoutActionBuilder {

  private String postLogoutRedirectParam;
  private final String clientId;

  public CiviformOidcLogoutActionBuilder(
      Config civiformConfiguration, OidcConfiguration oidcConfiguration, String clientId) {
    super(oidcConfiguration);
    checkNotNull(civiformConfiguration);
    // Use `post_logout_redirect_uri` by default according OIDC spec.
    this.postLogoutRedirectParam =
        getConfigurationValue(civiformConfiguration, "auth.oidc_post_logout_param")
            .orElse("post_logout_redirect_uri");

    this.clientId = clientId;
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

        LogoutRequest logoutRequest =
            new CustomOidcLogoutRequest(
                endSessionEndpoint,
                postLogoutRedirectParam,
                new URI(targetUrl),
                Optional.of(clientId),
                state);

        return Optional.of(
            HttpActionHelper.buildRedirectUrlAction(context, logoutRequest.toURI().toString()));
      } catch (URISyntaxException e) {
        throw new TechnicalException(e);
      }
    }

    return Optional.empty();
  }
}
