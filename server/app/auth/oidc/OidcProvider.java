package auth.oidc;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileFactory;
import com.typesafe.config.Config;
import javax.inject.Provider;
import org.pac4j.core.http.callback.PathParameterCallbackUrlResolver;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.UserRepository;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.Optional;

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

  public OidcProvider(
      Config configuration,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    this.configuration = checkNotNull(configuration);
    this.profileFactory = checkNotNull(profileFactory);
    this.applicantRepositoryProvider = checkNotNull(applicantRepositoryProvider);

    this.baseUrl = getConfigurationValue("base_url").orElseThrow();
  }

  /*
   * The prefix used in the application.conf for retriving oidc options.
   */
  protected abstract String attributePrefix();

  /*
   * The OIDC Provider Name (optional).
   */
  protected abstract Optional<String> getProviderName();

  /*
   * Provide the profile adaptor that should be used.
   */
  public abstract ProfileCreator getProfileAdapter(OidcConfiguration config, OidcClient client);

  /*
   * The OIDC Client ID.
   */
  protected abstract String getClientID();

  /*
   * The OIDC Client Secret.
   */
  protected abstract String getClientSecret();

  /*
   * The OIDC Discovery URI.
   */
  protected abstract String getDiscoveryURI();

  /*
   * The OIDC Response Mode.
   */
  protected abstract String getResponseMode();

  /*
   * The OIDC Response Type.
   */
  protected abstract String getResponseType();

  /*
   * Default scopes for this OIDC implementation.
   */
  protected abstract ImmutableList<String> getDefaultScopes();

  /*
   * Any application.conf-defined extra scopes for this OIDC implementation.
   */
  protected abstract ImmutableList<String> getExtraScopes();

  /*
   * Helper function for retriving values from the application.conf,
   * prepended with "<attributePrefix>."
   */
  protected final Optional<String> getConfigurationValue(String suffix) {
    String name = attributePrefix() + "." + suffix;
    if (configuration.hasPath(name)) {
      return Optional.ofNullable(configuration.getString(name));
    }
    return Optional.empty();
  }

  protected String getCallbackURL() {
    return baseUrl + "/callback";
  }

  private final String getScope() {
    // Scopes are the other things that we want from the OIDC endpoint
    // (needs to also be configured on provider side).
    ImmutableList<String> allClaims = ImmutableList.<String>builder()
        .addAll(getDefaultScopes())
        .addAll(getExtraScopes())
        .build();
    return String.join(" ", allClaims);
  }

  @Override
  public OidcClient get() {
    String clientID = getClientID();
    String clientSecret = getClientSecret();
    String discoveryURI = getDiscoveryURI();
    String responseMode = getResponseMode();
    String responseType = getResponseType();
    String callbackURL = getCallbackURL();
    String scopes = getScope();
    var requiredAttributes = ImmutableList.of(
        clientID,
        clientSecret,
        discoveryURI,
        responseMode,
        responseType,
        callbackURL);
    // Check that none are null or blank.
    if (requiredAttributes.stream().map(Strings::nullToEmpty).anyMatch(String::isBlank)) {
      logger.error(
          String.format(
              "Can't get OIDC client - Missing some required Provider data: "
                  + "clientID=%s, "
                  + "clientSecret=%s, "
                  + "discoveryURI=%s, "
                  + "responseMode=%s, "
                  + "responseType=%s, "
                  + "callbackURL=%s",
              clientID,
              clientSecret,
              discoveryURI,
              responseMode,
              responseType,
              callbackURL));
      return null;
    }
    Optional<String> providerName = getProviderName();
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

    if (!providerName.orElse("").isBlank()) {
      client.setName(providerName.get());
    }

    client.setCallbackUrl(callbackURL);
    client.setProfileCreator(getProfileAdapter(config, client));
    client.setCallbackUrlResolver(new PathParameterCallbackUrlResolver());
    client.init();
    return client;
  }
}
