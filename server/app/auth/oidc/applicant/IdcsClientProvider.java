package auth.oidc.applicant;

import auth.oidc.OidcClientProvider;
import auth.oidc.OidcClientProviderParams;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.Optional;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;

/** This class customized the OIDC provider to a specific provider, allowing overrides to be set. */
public final class IdcsClientProvider extends OidcClientProvider {

  private static final String ATTRIBUTE_PREFIX = "idcs.";
  private static final String CLIENT_ID_CONFIG_NAME = "client_id";
  private static final String CLIENT_SECRET_CONFIG_NAME = "secret";
  private static final String DISCOVERY_URI_CONFIG_NAME = "discovery_uri";

  private static final ImmutableList<String> DEFAULT_SCOPES =
      ImmutableList.of("openid", "profile", "email");

  @Inject
  public IdcsClientProvider(OidcClientProviderParams params) {
    super(params);
  }

  @Override
  protected String attributePrefix() {
    return ATTRIBUTE_PREFIX;
  }

  @Override
  protected Optional<String> getProviderName() {
    return Optional.empty();
  }

  @Override
  public ProfileCreator getProfileCreator(OidcConfiguration config, OidcClient client) {
    return new IdcsApplicantProfileCreator(config, client, params);
  }

  @Override
  protected String getClientID() {
    return getConfigurationValueOrThrow(CLIENT_ID_CONFIG_NAME);
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
    return "form_post";
  }

  @Override
  protected String getResponseType() {
    // Our fake IDCS doesn't support 'token' auth.
    // https://github.com/panva/node-oidc-provider/blob/main/docs/README.md#responsetypes
    // We disable this auth for scenarios where the server is being run locally on a dev machine
    // and for browser test scenarios, where the server is accessible at the "civiform" host.
    // TODO: turn this into it's own fake provider or make the fake provider allow
    // 'token'.
    if (baseUrl.contains("localhost:") || baseUrl.startsWith("http://civiform:")) {
      return "id_token";
    }
    return "id_token token";
  }

  @Override
  protected ImmutableList<String> getDefaultScopes() {
    return DEFAULT_SCOPES;
  }

  @Override
  protected ImmutableList<String> getExtraScopes() {
    return ImmutableList.of();
  }

  @Override
  protected boolean getUseCsrf() {
    return false;
  }
}
