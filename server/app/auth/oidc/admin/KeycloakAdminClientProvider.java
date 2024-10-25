package auth.oidc.admin;

import auth.oidc.OidcClientProviderParams;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import java.util.Optional;
import org.pac4j.oidc.config.KeycloakOidcConfiguration;
import org.pac4j.oidc.config.OidcConfiguration;

public class KeycloakAdminClientProvider extends GenericOidcClientProvider {
  @Inject
  public KeycloakAdminClientProvider(OidcClientProviderParams params) {
    super(params);
  }

  @Override
  @VisibleForTesting
  public String attributePrefix() {
    return "admin_generic_oidc_";
  }

  @Override
  protected String getResponseMode() {
    return "form_post";
  }

  @Override
  protected String getResponseType() {
    return "id_token token";
  }

  @Override
  protected Optional<String> getProviderName() {
    return Optional.of("keycloak-admin");
  }

  @Override
  public OidcConfiguration getConfig() {
    KeycloakOidcConfiguration keycloakConfig = new KeycloakOidcConfiguration();

    OidcConfiguration config = super.getConfig();
    keycloakConfig.setClientId(config.getClientId());
    keycloakConfig.setSecret(config.getSecret());
    keycloakConfig.setDiscoveryURI(config.getDiscoveryURI());
    keycloakConfig.setResponseType(getResponseType());
    keycloakConfig.setResponseMode(getResponseMode());
    keycloakConfig.setRealm("admin-realm");
    keycloakConfig.setBaseUri("http://auth:8080");
    keycloakConfig.setAllowUnsignedIdTokens(true);
    keycloakConfig.setUseNonce(true);
    keycloakConfig.setDisablePkce(true);
    keycloakConfig.setClientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
    keycloakConfig.setPreferredJwsAlgorithm(JWSAlgorithm.RS256);

    return keycloakConfig;
  }
}
