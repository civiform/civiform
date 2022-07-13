package auth.oidc;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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

  public OidcProvider(
      Config configuration,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    this.configuration = checkNotNull(configuration);
    this.profileFactory = checkNotNull(profileFactory);
    this.applicantRepositoryProvider = checkNotNull(applicantRepositoryProvider);

    this.baseUrl =
        getBaseConfigurationValue("base_url")
            .orElseThrow(() -> new RuntimeException("base_url must be set"));
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
    return getBaseConfigurationValue(name);
  }

  /*
   * Helper function for retriving values from the application.conf,
   * prepended with "<attributePrefix>."
   */
  protected final String getConfigurationValueOrThrow(String suffix) {
    String name = attributePrefix() + "." + suffix;
    return getBaseConfigurationValue(name)
        .orElseThrow(() -> new RuntimeException(name + " must be set"));
  }

  /*
   * Helper function for retriving values from the application.conf,
   * prepended with "<attributePrefix>."
   */
  protected final Optional<String> getBaseConfigurationValue(String name) {
    if (configuration.hasPath(name)) {
      return Optional.ofNullable(configuration.getString(name));
    }
    return Optional.empty();
  }

  protected String getCallbackURL() {
    return baseUrl + "/callback";
  }

  /*
   * Helper function for combining the default and additional scopes,
   * and return them in the space-seperated string required bu OIDC.
   */
  @VisibleForTesting
  public final String getScopesAttribute() {
    // Scopes are the other things that we want from the OIDC endpoint
    // (needs to also be configured on provider side).
    return ImmutableSet.<String>builder()
        .addAll(getDefaultScopes())
        .addAll(getExtraScopes())
        .build()
        .stream()
        .collect(Collectors.joining(" "));
  }

  @Override
  public OidcClient get() {
    String clientID = getClientID();
    String clientSecret = getClientSecret();
    String discoveryURI = getDiscoveryURI();
    String responseMode = getResponseMode();
    String responseType = getResponseType();
    String callbackURL = getCallbackURL();
    String scope = getScopesAttribute();
    var missingData =
        ImmutableMap.<String, String>builder()
            .put("clientID", clientID)
            .put("clientSecret", clientSecret)
            .put("discoveryURI", discoveryURI)
            .put("responseMode", responseMode)
            .put("responseType", responseType)
            .put("callbackURL", callbackURL)
            .build()
            .entrySet()
            .stream()
            .filter(e -> Strings.isNullOrEmpty(e.getValue()))
            .map(Map.Entry::getKey)
            .collect(ImmutableList.toImmutableList());

    // Check that none are null or blank.
    if (missingData.size() > 0) {
      throw new RuntimeException(
          "Missing OIDC attributes " + missingData.stream().collect(Collectors.joining(", ")));
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

    config.setScope(scope);
    OidcClient client = new OidcClient(config);

    if (providerName.isPresent()) {
      client.setName(providerName.get());
    }

    client.setCallbackUrl(callbackURL);
    client.setProfileCreator(getProfileAdapter(config, client));
    client.setCallbackUrlResolver(new PathParameterCallbackUrlResolver());
    try {
      client.init();
    } catch (Exception e) {
      logger.error("Error while initilizing OIDC provider", e);
      throw e;
    }

    var providerMetadata = client.getConfiguration().getProviderMetadata();
    logger.debug("Provider metadata: " + providerMetadata.toString());
    if (providerMetadata.supportsAuthorizationResponseIssuerParam()
        && responseMode.equals("form_post")
        && responseType.contains("token")
        && !responseType.contains("code")) {
      // The issuer param verification doesn't work for form_post token/id_token response types.
      providerMetadata.setSupportsAuthorizationResponseIssuerParam(false);
      logger.debug("Disabled authorization_response_iss_parameter_supported");
    }
    return client;
  }
}
