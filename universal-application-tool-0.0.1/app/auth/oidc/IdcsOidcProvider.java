package auth.oidc;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ApplicantAuthClient;
import auth.ProfileFactory;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import javax.inject.Provider;
import org.pac4j.core.http.callback.PathParameterCallbackUrlResolver;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.springframework.lang.Nullable;
import repository.UserRepository;

public class IdcsOidcProvider implements Provider<OidcClient> {

  private final com.typesafe.config.Config configuration;
  private final String baseUrl;
  private final ProfileFactory profileFactory;
  private final Provider<UserRepository> applicantRepositoryProvider;

  @Inject
  public IdcsOidcProvider(
      com.typesafe.config.Config configuration,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    this.configuration = checkNotNull(configuration);
    this.baseUrl = configuration.getString("base_url");
    this.profileFactory = checkNotNull(profileFactory);
    this.applicantRepositoryProvider = applicantRepositoryProvider;
  }

  @Override
  @Provides
  @Singleton
  @Nullable
  @ApplicantAuthClient
  public OidcClient get() {
    if (!this.configuration.hasPath("idcs.client_id")
        || !this.configuration.hasPath("idcs.secret")) {
      return null;
    }
    OidcConfiguration config = new OidcConfiguration();
    config.setClientId(this.configuration.getString("idcs.client_id"));
    config.setSecret(this.configuration.getString("idcs.secret"));
    config.setDiscoveryURI(this.configuration.getString("idcs.discovery_uri"));
    config.setResponseMode("form_post");
    // Our local fake IDCS doesn't support 'token' auth.
    if (baseUrl.contains("localhost:")) {
      config.setResponseType("id_token");
    } else {
      config.setResponseType("id_token token");
    }
    config.setUseNonce(true);
    config.setWithState(false);
    config.setScope("openid profile email");
    OidcClient client = new OidcClient(config);
    client.setCallbackUrl(baseUrl + "/callback");
    client.setProfileCreator(
        new IdcsProfileAdapter(config, client, profileFactory, applicantRepositoryProvider));
    client.setCallbackUrlResolver(new PathParameterCallbackUrlResolver());
    client.init();
    return client;
  }
}
