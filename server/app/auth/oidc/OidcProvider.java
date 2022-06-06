package auth.oidc;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileFactory;
import com.typesafe.config.Config;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import javax.inject.Provider;
import org.pac4j.core.http.callback.PathParameterCallbackUrlResolver;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.UserRepository;

/**
 * This class provides the base applicant OIDC implementation. It's abstract because AD and other
 * providers need slightly different implementations and profile adaptors, and use different config
 * values.
 */
public abstract class OidcProvider implements Provider<OidcClient> {

  private static final Logger logger = LoggerFactory.getLogger(OidcProvider.class);
  protected final Config configuration;
  protected final ProfileFactory profileFactory;
  protected final Provider<UserRepository> applicantRepositoryProvider;
  protected final String baseUrl;

  protected String[] defaultScopes = {"openid", "profile", "email"};

  private String providerNameConfigName = "provider_name";
  private String clientIDConfigName = "client_id";
  private String clientSecretConfigName = "client_secret";
  private String discoveryURIConfigName = "discovery_uri";
  private String responseModeConfigName = "response_mode";
  private String extraScopesConfigName = "additional_scopes";

  public OidcProvider(
      Config configuration,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    this.configuration = checkNotNull(configuration);
    this.profileFactory = checkNotNull(profileFactory);
    this.applicantRepositoryProvider = applicantRepositoryProvider;

    baseUrl = getConfigurationValue("base_url");
  }

  /*
   * Provide the prefix used in the application.conf
   */
  protected abstract String attributePrefix();

  /*
   * Provide the profile adaptor that should be used.
   */
  public abstract ProfileCreator getProfileAdapter(OidcConfiguration config, OidcClient client);

  protected String getConfigurationValue(String attr, String defaultValue) {
    if (configuration.hasPath(attr)) {
      return configuration.getString(attr);
    }
    return defaultValue;
  }

  protected String getConfigurationValue(String attr) {
    return getConfigurationValue(attr, "");
  }

  protected String getProviderName() {
    return getConfigurationValue(attributePrefix() + "." + providerNameConfigName);
  }

  protected String getClientID() {
    return getConfigurationValue(attributePrefix() + "." + clientIDConfigName);
  }

  protected String getClientSecret() {
    return getConfigurationValue(attributePrefix() + "." + clientSecretConfigName);
  }

  protected String getDiscoveryURI() {
    return getConfigurationValue(attributePrefix() + "." + discoveryURIConfigName);
  }

  protected String getResponseMode() {
    return getConfigurationValue(attributePrefix() + "." + responseModeConfigName);
  }

  protected String getResponseType() {
    // Our local fake IDCS doesn't support 'token' auth.
    if (baseUrl.contains("localhost:")) {
      return "id_token";
    }
    return "id_token token";
  }

  protected String[] getExtraScopes() {
    return getConfigurationValue(attributePrefix() + "." + extraScopesConfigName).split(" ");
  }

  protected String getCallbackURL() {
    return baseUrl + "/callback";
  }

  protected String getScopes() {
    // Scopes are the other things that we want from the OIDC endpoint
    // (needs to also be configured on provider side).
    String[] extraScopes = getExtraScopes();

    ArrayList<String> allClaims = new ArrayList<>();
    Collections.addAll(allClaims, defaultScopes);
    Collections.addAll(allClaims, extraScopes);
    return String.join(" ", allClaims);
  }

  @Override
  public OidcClient get() {
    String clientID = Objects.toString(getClientID(), "");
    String clientSecret = Objects.toString(getClientSecret(), "");
    String discoveryURI = Objects.toString(getDiscoveryURI(), "");
    String responseMode = Objects.toString(getResponseMode(), "");
    String responseType = Objects.toString(getResponseType(), "");
    String callbackURL = Objects.toString(getCallbackURL(), "");
    String providerName = Objects.toString(getProviderName(), ""); // optional
    String scopes = Objects.toString(getScopes(), "");
    if (clientID.isBlank()
        || clientSecret.isBlank()
        || discoveryURI.isBlank()
        || responseMode.isBlank()
        || responseType.isBlank()
        || callbackURL.isBlank()
        || baseUrl.isBlank()) {
      logger.error(
          String.format(
              "Can't get OIDC client - Missing Provider data:\n"
                  + " baseUrl=%s\n"
                  + " clientID=%s \n"
                  + " clientSecret=%s \n"
                  + " discoveryURI=%s \n"
                  + " responseMode=%s \n"
                  + " responseType=%s \n"
                  + " callbackURL=%s \n"
                  + " providerName=%s",
              baseUrl,
              clientID,
              clientSecret,
              discoveryURI,
              responseMode,
              responseType,
              callbackURL,
              providerName));
      return null;
    }
    OidcConfiguration config = new OidcConfiguration();

    config.setClientId(clientID);
    config.setSecret(clientSecret);
    config.setDiscoveryURI(discoveryURI);
    // Tells the OIDC provider what type of response to use when it sends info back
    // from the auth request.
    config.setResponseMode(responseMode);
    config.setResponseType(responseType);

    config.setUseNonce(true);
    config.setWithState(false);

    config.setScope(scopes);

    OidcClient client = new OidcClient(config);

    if (!providerName.isBlank()) {
      client.setName(providerName);
    }

    client.setCallbackUrl(callbackURL);
    client.setProfileCreator(getProfileAdapter(config, client));
    client.setCallbackUrlResolver(new PathParameterCallbackUrlResolver());
    client.init();
    return client;
  }
}
