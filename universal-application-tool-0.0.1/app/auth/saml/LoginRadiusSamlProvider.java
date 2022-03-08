package auth.saml;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ApplicantAuthClient;
import auth.ProfileFactory;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import javax.inject.Provider;
import org.pac4j.core.http.callback.PathParameterCallbackUrlResolver;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import org.springframework.lang.Nullable;
import repository.UserRepository;

public class LoginRadiusSamlProvider implements Provider<SAML2Client> {

  private final com.typesafe.config.Config configuration;
  private final ProfileFactory profileFactory;
  private final Provider<UserRepository> applicantRepositoryProvider;
  private final String baseUrl;

  @Inject
  public LoginRadiusSamlProvider(
      com.typesafe.config.Config configuration,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    this.configuration = checkNotNull(configuration);
    this.profileFactory = checkNotNull(profileFactory);
    this.baseUrl = configuration.getString("base_url");
    this.applicantRepositoryProvider = checkNotNull(applicantRepositoryProvider);
  }

  @Override
  @Provides
  @Singleton
  @Nullable
  @ApplicantAuthClient
  public SAML2Client get() {
    if (!this.configuration.hasPath("login_radius.keystore_password")
        || !this.configuration.hasPath("login_radius.private_key_password")
        || !this.configuration.hasPath("login_radius.api_key")) {
      return null;
    }

    String metadataResourceUrl =
        String.format(
            "%s?apikey=%s&appName=%s",
            this.configuration.getString("login_radius.metadata_uri"),
            this.configuration.getString("login_radius.api_key"),
            this.configuration.getString("login_radius.saml_app_name"));
    SAML2Configuration config = new SAML2Configuration();
    config.setKeystoreResourceFilepath(this.configuration.getString("login_radius.keystore_name"));
    config.setKeystorePassword(this.configuration.getString("login_radius.keystore_password"));
    config.setPrivateKeyPassword(this.configuration.getString("login_radius.private_key_password"));
    config.setIdentityProviderMetadataResourceUrl(metadataResourceUrl);
    SAML2Client client = new SAML2Client(config);

    client.setProfileCreator(
        new SamlCiviFormProfileAdapter(
            config, client, profileFactory, applicantRepositoryProvider));

    client.setCallbackUrlResolver(new PathParameterCallbackUrlResolver());
    client.setCallbackUrl(baseUrl + "/callback");
    client.init();
    return client;
  }
}
