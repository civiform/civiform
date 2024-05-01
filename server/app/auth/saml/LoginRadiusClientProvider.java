package auth.saml;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileFactory;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.util.IllegalFormatException;
import java.util.Optional;
import javax.inject.Provider;
import org.pac4j.core.http.callback.PathParameterCallbackUrlResolver;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.AccountRepository;

// TODO(#3856): Update with a non deprecated saml impl.
@SuppressWarnings("deprecation")
public class LoginRadiusClientProvider implements Provider<SAML2Client> {

  private static final Logger logger = LoggerFactory.getLogger(LoginRadiusClientProvider.class);

  private final Config configuration;
  private final ProfileFactory profileFactory;
  private final Provider<AccountRepository> applicantRepositoryProvider;
  private final String baseUrl;

  @Inject
  public LoginRadiusClientProvider(
      Config configuration,
      ProfileFactory profileFactory,
      Provider<AccountRepository> applicantRepositoryProvider) {
    this.configuration = checkNotNull(configuration);
    this.profileFactory = checkNotNull(profileFactory);
    this.baseUrl = configuration.getString("base_url");
    this.applicantRepositoryProvider = checkNotNull(applicantRepositoryProvider);
  }

  @Override
  public SAML2Client get() {
    if (!configuration.hasPath("login_radius.keystore_password")
        || !configuration.hasPath("login_radius.private_key_password")
        || !configuration.hasPath("login_radius.api_key")) {
      return null;
    }

    Optional<String> metadataResourceUrlOpt = formatMetadataResourceUrl();

    if (metadataResourceUrlOpt.isEmpty()) {
      logger.warn("Invalid SAML metadata resource URL generated in LoginRadiusSamlProvider");
      return null;
    }

    String metadataResourceUrl = metadataResourceUrlOpt.get();
    SAML2Configuration config = new SAML2Configuration();
    config.setKeystoreResourceFilepath(configuration.getString("login_radius.keystore_name"));
    config.setKeystorePassword(configuration.getString("login_radius.keystore_password"));
    config.setPrivateKeyPassword(configuration.getString("login_radius.private_key_password"));
    config.setIdentityProviderMetadataResourceUrl(metadataResourceUrl);
    SAML2Client client = new SAML2Client(config);

    client.setProfileCreator(
        new SamlProfileCreator(config, client, profileFactory, applicantRepositoryProvider));

    client.setCallbackUrlResolver(new PathParameterCallbackUrlResolver());
    client.setCallbackUrl(baseUrl + "/callback");
    client.init();
    return client;
  }

  private Optional<String> formatMetadataResourceUrl() {
    try {
      String metadataResourceUrl =
          String.format(
              "%s?apikey=%s&appName=%s",
              configuration.getString("login_radius.metadata_uri"),
              configuration.getString("login_radius.api_key"),
              configuration.getString("login_radius.saml_app_name"));
      return Optional.of(metadataResourceUrl);
    } catch (IllegalFormatException
        | NullPointerException
        | ConfigException.Missing
        | ConfigException.WrongType e) {
      return Optional.empty();
    }
  }
}
