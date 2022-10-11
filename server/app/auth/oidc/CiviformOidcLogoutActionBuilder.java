package auth.oidc;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfileData;
import com.google.common.collect.ImmutableMap;
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
import org.pac4j.core.util.generator.ValueGenerator;
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
  private ImmutableMap<String, String> extraParams;
  private Optional<ValueGenerator> stateGenerator = Optional.empty();

  public CiviformOidcLogoutActionBuilder(
      Config civiformConfiguration, OidcConfiguration oidcConfiguration, String clientID) {
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
  private static Optional<String> getConfigurationValue(Config civiformConfiguration, String name) {
    if (civiformConfiguration.hasPath(name)) {
      return Optional.ofNullable(civiformConfiguration.getString(name));
    }
    return Optional.empty();
  }

  public Optional<ValueGenerator> getStateGenerator() {
    return stateGenerator;
  }

  /**
   * If the OIDC provider requires the optional state param for logout (see
   * https://openid.net/specs/openid-connect-rpinitiated-1_0.html), set a state generator here. Note
   * that the state is not saved and validated by the client, so it does not achive the goal of
   * "maintain state between the logout request and the callback" as specified by the spec.
   */
  public CiviformOidcLogoutActionBuilder setStateGenerator(final ValueGenerator stateGenerator) {
    if (stateGenerator == null) {
      this.stateGenerator = Optional.empty();
    }
    this.stateGenerator = Optional.of(stateGenerator);
    return this;
  }

  /**
   * Additional url params to add to logout request. Some identity providers require including
   * client_id for example.
   */
  public CiviformOidcLogoutActionBuilder setExtraParams(ImmutableMap<String, String> extraParams) {
    this.extraParams = extraParams;
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
        // Optional state param for logout is only needed by certain OIDC providers.
        State state = null;
        if (getStateGenerator().isPresent()) {
          state = new State(getStateGenerator().get().generateValue(context, sessionStore));
        }

        LogoutRequest logoutRequest =
            new CustomOidcLogoutRequest(
                endSessionEndpoint,
                postLogoutRedirectParam.orElse(null),
                new URI(targetUrl),
                extraParams,
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
