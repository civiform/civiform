package auth.oidc.admin;

import auth.oidc.OidcClientProviderParams;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import java.util.Optional;
import org.pac4j.oidc.config.KeycloakOidcConfiguration;
import org.pac4j.oidc.config.OidcConfiguration;
import play.Environment;

/**
 * WARNING! This is EXPERIMENTAL only and not production ready
 *
 * <p>Customize the OIDC provider to handle Keycloak specific settings for the admin provider
 */
public class KeycloakAdminClientProvider extends GenericOidcClientProvider {
  @Inject
  public KeycloakAdminClientProvider(OidcClientProviderParams params, Environment env) {
    super(params);

    if (env.isProd()) {
      throw new UnsupportedOperationException(
          "Keycloak use is experimental and cannot be used in production environments at this"
              + " time.");
    }
  }

  @Override
  @VisibleForTesting
  public String attributePrefix() {
    return "keycloak.admin.";
  }

  @Override
  protected String getResponseMode() {
    return "form_post";
  }

  @Override
  protected String getResponseType() {
    return "id_token token";
  }

  protected String getRealm() {
    return getConfigurationValueOrThrow("realm");
  }

  protected String getBaseUri() {
    return getConfigurationValueOrThrow("base_uri");
  }

  @Override
  protected Optional<String> getProviderName() {
    return Optional.of("keycloak-admin");
  }

  @Override
  public OidcConfiguration getConfig() {
    KeycloakOidcConfiguration config = new KeycloakOidcConfiguration();

    config.setClientId(getClientID());
    config.setSecret(
        getClientSecret().orElseThrow(() -> new RuntimeException("client_secret must be set")));
    config.setDiscoveryURI(getDiscoveryURI());
    config.setResponseType(getResponseType());
    config.setResponseMode(getResponseMode());
    config.setRealm(getRealm());
    config.setBaseUri(getBaseUri());
    config.setAllowUnsignedIdTokens(true);
    config.setUseNonce(true);
    config.setDisablePkce(true);
    config.setClientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
    config.setPreferredJwsAlgorithm(JWSAlgorithm.RS256);

    return config;
  }
}
