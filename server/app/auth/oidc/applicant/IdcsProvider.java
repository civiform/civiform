package auth.oidc.applicant;

import auth.ProfileFactory;
import auth.oidc.OidcProvider;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.util.Optional;
import javax.inject.Provider;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import repository.UserRepository;

/** This class customized the OIDC provider to a specific provider, allowing overrides to be set. */
public final class IdcsProvider extends OidcProvider {

  private static final String ATTRIBUTE_PREFIX = "idcs";
  private static final String CLIENT_ID_CONFIG_NAME = "client_id";
  private static final String CLIENT_SECRET_CONFIG_NAME = "secret";
  private static final String DISCOVERY_URI_CONFIG_NAME = "discovery_uri";

  private static final ImmutableList<String> DEFAULT_SCOPES =
      ImmutableList.of("openid", "profile", "email");

  @Inject
  public IdcsProvider(
      Config configuration,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    super(configuration, profileFactory, applicantRepositoryProvider);
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
  public ProfileCreator getProfileAdapter(OidcConfiguration config, OidcClient client) {
    return new IdcsProfileAdapter(config, client, profileFactory, applicantRepositoryProvider);
  }

  @Override
  protected String getClientID() {
    return getConfigurationValue(CLIENT_ID_CONFIG_NAME)
        .orElseThrow(
            () ->
                new RuntimeException(
                    ATTRIBUTE_PREFIX + "." + CLIENT_ID_CONFIG_NAME + " must be set"));
  }

  @Override
  protected String getClientSecret() {
    return getConfigurationValue(CLIENT_SECRET_CONFIG_NAME)
        .orElseThrow(
            () ->
                new RuntimeException(
                    ATTRIBUTE_PREFIX + "." + CLIENT_SECRET_CONFIG_NAME + " must be set"));
  }

  @Override
  protected String getDiscoveryURI() {
    return getConfigurationValue(DISCOVERY_URI_CONFIG_NAME)
        .orElseThrow(
            () ->
                new RuntimeException(
                    ATTRIBUTE_PREFIX + "." + DISCOVERY_URI_CONFIG_NAME + " must be set"));
  }

  @Override
  protected String getResponseMode() {
    return "form_post";
  }

  @Override
  protected String getResponseType() {
    // Our local fake IDCS doesn't support 'token' auth.
    // https://github.com/panva/node-oidc-provider/blob/main/docs/README.md#responsetypes
    // TODO: turn this into it's own fake provider or make the local provider allow
    // 'token'.
    if (baseUrl.contains("localhost:")) {
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
    return ImmutableList.<String>of();
  }
}
