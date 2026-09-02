package auth.oidc.admin;

import auth.oidc.OidcClientProvider;
import auth.oidc.OidcClientProviderParams;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.Optional;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;

/**
 * This class implements a `Provider` of a generic `OidcClient` for use in authenticating and
 * authorizing admins.
 */
public class GenericOidcClientProvider extends OidcClientProvider {

  private static final String ATTRIBUTE_PREFIX = "admin_generic_oidc_";
  private static final ImmutableList<String> DEFAULT_SCOPES =
      ImmutableList.of("openid", "profile", "email");

  private static final String PROVIDER_NAME_CONFIG_NAME = "provider_name";
  private static final String CLIENT_ID_CONFIG_NAME = "client_id";
  private static final String CLIENT_SECRET_CONFIG_NAME = "client_secret";
  private static final String DISCOVERY_URI_CONFIG_NAME = "discovery_uri";
  private static final String RESPONSE_MODE_CONFIG_NAME = "response_mode";
  private static final String RESPONSE_TYPE_CONFIG_NAME = "response_type";
  private static final String EXTRA_SCOPES_CONFIG_NAME = "additional_scopes";
  private static final String USE_CSRF = "use_csrf";

  @Inject
  GenericOidcClientProvider(OidcClientProviderParams params) {
    super(params);
  }

  @Override
  @VisibleForTesting
  public String attributePrefix() {
    return ATTRIBUTE_PREFIX;
  }

  @Override
  protected Optional<String> getProviderName() {
    return getConfigurationValue(PROVIDER_NAME_CONFIG_NAME);
  }

  @Override
  public ProfileCreator getProfileCreator(OidcConfiguration config, OidcClient client) {
    return new GenericOidcProfileCreator(config, client, params);
  }

  @Override
  protected String getClientID() {
    return getConfigurationValue(CLIENT_ID_CONFIG_NAME).orElse("");
  }

  @Override
  protected Optional<String> getClientSecret() {
    return Optional.of(getConfigurationValueOrThrow(CLIENT_SECRET_CONFIG_NAME));
  }

  @Override
  protected String getDiscoveryURI() {
    return getConfigurationValueOrThrow(DISCOVERY_URI_CONFIG_NAME);
  }

  @Override
  protected String getResponseMode() {
    return getConfigurationValueOrThrow(RESPONSE_MODE_CONFIG_NAME);
  }

  @Override
  protected String getResponseType() {
    return getConfigurationValueOrThrow(RESPONSE_TYPE_CONFIG_NAME);
  }

  @Override
  protected ImmutableList<String> getDefaultScopes() {
    return DEFAULT_SCOPES;
  }

  @Override
  protected ImmutableList<String> getExtraScopes() {
    Optional<String> extraScopesMaybe = getConfigurationValue(EXTRA_SCOPES_CONFIG_NAME);
    return extraScopesMaybe
        .map(s -> ImmutableList.copyOf(s.split(" ")))
        .orElseGet(ImmutableList::of);
  }

  @Override
  protected boolean getUseCsrf() {
    return Boolean.valueOf(getConfigurationValueOrThrow(USE_CSRF));
  }
}
