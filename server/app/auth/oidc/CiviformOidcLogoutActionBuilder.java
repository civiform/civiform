package auth.oidc;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import com.typesafe.config.Config;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.core.util.HttpActionHelper;
import org.pac4j.core.util.generator.ValueGenerator;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.logout.OidcLogoutActionBuilder;
import org.pac4j.oidc.profile.OidcProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom OidcLogoutActionBuilder for CiviFormProfileData (since it extends CommonProfile, not
 * OidcProfile). Also allows for divergence from the [oidc
 * spec](https://openid.net/specs/openid-connect-rpinitiated-1_0.html) if your provider requires it
 * (e.g. Auth0).
 *
 * <p>Uses the post_logout_redirect_uri parameter by default, but allows overriding to a different
 * value using the auth.oidc_post_logout_param config variable
 *
 * <p>If the oidc_logout_client_id_param config variable is set, also adds the client_id to the
 * logout request.
 */
public final class CiviformOidcLogoutActionBuilder extends OidcLogoutActionBuilder {

  private static final Logger logger =
      LoggerFactory.getLogger(CiviformOidcLogoutActionBuilder.class);

  private String postLogoutRedirectParam;
  private Map<String, String> extraParams;
  private final ProfileFactory profileFactory;
  private Optional<ValueGenerator> stateGenerator = Optional.empty();

  public CiviformOidcLogoutActionBuilder(
      Config civiformConfiguration,
      OidcConfiguration oidcConfiguration,
      ProfileFactory profileFactory,
      String clientID) {
    super(oidcConfiguration);
    checkNotNull(civiformConfiguration);
    // Use `post_logout_redirect_uri` by default according OIDC spec.
    this.postLogoutRedirectParam =
        getConfigurationValue(civiformConfiguration, "auth.oidc_post_logout_param")
            .orElse("post_logout_redirect_uri");
    Optional<String> clientIdParam =
        getConfigurationValue(civiformConfiguration, "auth.oidc_logout_client_id_param");

    this.profileFactory = checkNotNull(profileFactory);
    this.extraParams = new HashMap<>();

    if (clientIdParam.isPresent()) {
      this.extraParams.put(clientIdParam.get(), clientID);
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
    ProfileUtils profileUtils = new ProfileUtils(sessionStore, profileFactory);

    // This is returning Optional.empty
    Optional<CiviFormProfile> civiFormProfile = profileUtils.currentUserProfile(context);
    ProfileManager profileManager = new ProfileManager(context, sessionStore);
    Optional<OidcProfile> oidcProfile = profileManager.getProfile(OidcProfile.class);
    logger.warn("DEBUG LOGOUT: getLogoutAction 1, oidcProfile = {}", oidcProfile);

    // CiviFormProfileData civiFormProfileData = (CiviFormProfileData) currentProfile;
    // final Optional<JWT> idTokenNew = civiFormProfileData.getIdToken();

    // logger.warn("DEBUG LOGOUT: getLogoutAction 1.1, idTokenNew= {}", idTokenNew);
    //
    // idTokenNew.ifPresent(idToken -> extraParams.put("id_token_hint", idToken.serialize()));

    if (civiFormProfile.isPresent()
        && civiFormProfile.get().getProfileData().getIdToken().isPresent()) {
      logger.warn("DEBUG LOGOUT: getLogoutAction 2");
      JWT idToken = civiFormProfile.get().getProfileData().getIdToken().get();
      logger.warn("DEBUG LOGOUT: getLogoutAction 3, idToken={}", idToken.serialize());
      extraParams.put("id_token_hint", idToken.serialize());
    } else {
      logger.warn(
          "DEBUG LOGOUT: getLogoutAction 3.1, profile present={}", civiFormProfile.isPresent());
    }

    String logoutUrl = configuration.findLogoutUrl();
    if (CommonHelper.isNotBlank(logoutUrl) && currentProfile instanceof CiviFormProfileData) {
      logger.warn("DEBUG LOGOUT: getLogoutAction 4");
      try {
        URI endSessionEndpoint = new URI(logoutUrl);
        // Optional state param for logout is only needed by certain OIDC providers.
        State state = null;
        if (getStateGenerator().isPresent()) {
          logger.warn("DEBUG LOGOUT: getLogoutAction 5");
          state = new State(getStateGenerator().get().generateValue(context, sessionStore));
        }

        logger.warn("DEBUG LOGOUT: getLogoutAction 6, extraParams={}", extraParams);
        LogoutRequest logoutRequest =
            new CustomOidcLogoutRequest(
                endSessionEndpoint,
                postLogoutRedirectParam,
                new URI(targetUrl),
                ImmutableMap.copyOf(extraParams),
                state);

        logger.warn("DEBUG LOGOUT: getLogoutAction 7");
        return Optional.of(
            HttpActionHelper.buildRedirectUrlAction(context, logoutRequest.toURI().toString()));
      } catch (URISyntaxException e) {
        logger.warn("DEBUG LOGOUT: getLogoutAction 8");
        throw new TechnicalException(e);
      }
    }

    return Optional.empty();
  }
}
