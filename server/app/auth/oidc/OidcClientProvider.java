package auth.oidc;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.IdentityProviderType;
import auth.ProfileFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.nimbusds.jose.JWSAlgorithm;
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
import repository.AccountRepository;
import services.settings.SettingsManifest;

/**
 * This class provides the base user OIDC implementation. It's abstract because AD and other
 * providers need slightly different implementations and profile creators, and use different config
 * values.
 */
public abstract class OidcClientProvider implements Provider<OidcClient> {

  private static final Logger logger = LoggerFactory.getLogger(OidcClientProvider.class);
  protected final OidcClientProviderParams params;
  protected final Config civiformConfig;
  protected final ProfileFactory profileFactory;
  protected final Provider<AccountRepository> accountRepositoryProvider;
  protected final String baseUrl;
  protected final SettingsManifest settingsManifest;

  public OidcClientProvider(OidcClientProviderParams params) {
    this.params = params;
    this.civiformConfig = checkNotNull(params.configuration());
    this.profileFactory = checkNotNull(params.profileFactory());
    this.accountRepositoryProvider = checkNotNull(params.accountRepositoryProvider());

    this.baseUrl =
        getBaseConfigurationValue("base_url")
            .orElseThrow(() -> new RuntimeException("base_url must be set"));
    this.settingsManifest = new SettingsManifest(this.civiformConfig);
  }

  /*
   * The prefix used in the application.conf for retrieving oidc options.
   *
   * If a '.' is intended to be included in the attribute name, it must be included at the end of the prefix.
   */
  protected abstract String attributePrefix();

  /*
   * The OIDC Provider Name (optional).
   */
  protected abstract Optional<String> getProviderName();

  /*
   * Provide the profile creator that should be used.
   */
  public abstract ProfileCreator getProfileCreator(OidcConfiguration config, OidcClient client);

  /*
   * The OIDC Client ID.
   */
  protected abstract String getClientID();

  /*
   * The OIDC Client Secret.
   */
  protected abstract Optional<String> getClientSecret();

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
   * Whether the `state` CSRF parameter should be set in the requests.
   */
  protected abstract boolean getUseCsrf();

  /*
   * Helper function for retrieving values from the application.conf,
   * prepended with <attributePrefix>
   */
  protected final Optional<String> getConfigurationValue(String suffix) {
    String name = attributePrefix() + suffix;
    return getBaseConfigurationValue(name);
  }

  /*
   * Helper function for retrieving values from the application.conf,
   * prepended with <attributePrefix>
   */
  protected final String getConfigurationValueOrThrow(String suffix) {
    String name = attributePrefix() + suffix;
    return getBaseConfigurationValue(name)
        .orElseThrow(() -> new RuntimeException(name + " must be set"));
  }

  /*
   * Helper function for retrieving values from the application.conf.
   */
  protected final Optional<String> getBaseConfigurationValue(String name) {
    if (civiformConfig.hasPath(name)) {
      return Optional.ofNullable(civiformConfig.getString(name));
    }
    return Optional.empty();
  }

  protected String getCallbackURL() {
    return baseUrl + "/callback";
  }

  protected Optional<String> getLogoutURL() {
    return getBaseConfigurationValue("auth.oidc_override_logout_url");
  }

  /*
   * Helper function for combining the default and additional scopes,
   * and return them in the space-separated string required by OIDC.
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

  public OidcConfiguration getConfig() {
    String clientID = getClientID();
    Optional<String> clientSecretMaybe = getClientSecret();
    String discoveryURI = getDiscoveryURI();
    String responseMode = getResponseMode();
    String responseType = getResponseType();
    String scope = getScopesAttribute();
    var missingData =
        ImmutableMap.<String, String>builder()
            .put("clientID", clientID)
            .put("discoveryURI", discoveryURI)
            .put("responseMode", responseMode)
            .put("responseType", responseType)
            .build()
            .entrySet()
            .stream()
            .filter(e -> Strings.isNullOrEmpty(e.getValue()))
            .map(Map.Entry::getKey)
            .collect(ImmutableList.toImmutableList());

    // Check that none are null or blank.
    if (missingData.size() > 0) {
      throw new RuntimeException(
          "Missing OIDC config attributes "
              + missingData.stream().collect(Collectors.joining(", ")));
    }
    OidcConfiguration config = new OidcConfiguration();

    getLogoutURL().ifPresent(config::setLogoutUrl);
    config.setClientId(clientID);

    if (clientSecretMaybe.isPresent() && !Strings.isNullOrEmpty(clientSecretMaybe.get())) {
      config.setSecret(clientSecretMaybe.get());
    }

    config.setDiscoveryURI(discoveryURI);

    // Tells the OIDC provider what type of response to use when it sends info back
    // from the auth request.
    config.setResponseMode(responseMode);
    config.setResponseType(responseType);
    config.setPreferredJwsAlgorithm(JWSAlgorithm.RS256);

    config.setUseNonce(true);
    config.setWithState(getUseCsrf());

    config.setScope(scope);
    return config;
  }

  @Override
  public OidcClient get() {
    String callbackURL = getCallbackURL();
    if (Strings.isNullOrEmpty(callbackURL)) {
      throw new RuntimeException("Missing OIDC client attribute: callbackURL");
    }

    OidcConfiguration config = getConfig();
    OidcClient client = new OidcClient(config);

    Optional<String> providerName = getProviderName();
    if (providerName.isPresent()) {
      client.setName(providerName.get());
    }
    client.setCallbackUrl(callbackURL);
    ProfileCreator profileCreator = getProfileCreator(config, client);
    IdentityProviderType identityProviderType =
        ((CiviformOidcProfileCreator) profileCreator).identityProviderType();
    client.setProfileCreator(profileCreator);
    client.setCallbackUrlResolver(new PathParameterCallbackUrlResolver());
    client.setLogoutActionBuilder(
        new CiviformOidcLogoutActionBuilder(
            config, config.getClientId(), params, identityProviderType));

    try {
      client.init();
    } catch (RuntimeException e) {
      logger.error("Error while initializing OIDC provider", e);
      throw e;
    }
    var providerMetadata = client.getConfiguration().getOpMetadataResolver().load();
    String responseMode = config.getResponseMode();
    String responseType = config.getResponseType();
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
