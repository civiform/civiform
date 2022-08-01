package auth.oidc;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfileData;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import com.typesafe.config.Config;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CiviformOidcLogoutActionBuilder extends OidcLogoutActionBuilder {
  private static final Logger logger =
      LoggerFactory.getLogger(CiviformOidcLogoutActionBuilder.class);

  protected final Config civiformConfiguration;
  protected final Optional<String> postLogoutRedirectParam;
  protected final Map<String, String> extraParams;
  protected final String baseUrl;

  public CiviformOidcLogoutActionBuilder(
      Config civiformConfiguration, final OidcConfiguration oidcConfiguration, String clientID) {
    super(oidcConfiguration);
    this.civiformConfiguration = checkNotNull(civiformConfiguration);
    this.postLogoutRedirectParam = getConfigurationValue("auth.oidc_post_logout_param");
    Optional<String> clientIdParam = getConfigurationValue("auth.oidc_logout_client_id_param");

    if (clientIdParam.isPresent()) {
      this.extraParams = Map.of(clientIdParam.get(), clientID);
    } else {
      this.extraParams = Map.of();
    }

    this.baseUrl =
        getConfigurationValue("base_url")
            .orElseThrow(() -> new RuntimeException("base_url must be set"));
  }

  /** Helper function for retriving values from the application.conf, */
  protected final Optional<String> getConfigurationValue(String name) {
    if (civiformConfiguration.hasPath(name)) {
      return Optional.ofNullable(civiformConfiguration.getString(name));
    }
    return Optional.empty();
  }

  // Override, since parent checks that the profile is an instance of OidcProfile.
  // Does not supply idtoken hint.
  @Override
  public Optional<RedirectionAction> getLogoutAction(
      final WebContext context,
      final SessionStore sessionStore,
      final UserProfile currentProfile,
      final String targetUrl) {
    final String logoutUrl = configuration.findLogoutUrl();
    logger.debug("Logging user out: " + currentProfile.toString());
    if (CommonHelper.isNotBlank(logoutUrl) && currentProfile instanceof CiviFormProfileData) {
      try {
        final URI endSessionEndpoint = new URI(logoutUrl);
        LogoutRequest logoutRequest =
            new CustomOidcLogoutRequest(
                endSessionEndpoint,
                this.postLogoutRedirectParam.orElse(null),
                new URI(targetUrl),
                this.extraParams);
        logger.debug("logoutRequest: " + logoutRequest.toURI().toString());

        return Optional.of(
            HttpActionHelper.buildRedirectUrlAction(context, logoutRequest.toURI().toString()));
      } catch (final URISyntaxException e) {
        throw new TechnicalException(e);
      }
    }

    return Optional.empty();
  }
}
