package auth.oidc.applicant;

import auth.ProfileFactory;
import auth.oidc.OidcProvider;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import javax.inject.Provider;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import repository.UserRepository;

/** This class customized the OIDC provider to a specific provider, allowing overrides to be set. */
public class IdcsProvider extends OidcProvider {
  protected String attributePrefix = "idcs";

  @Inject
  public IdcsProvider(
      Config configuration,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    super(configuration, profileFactory, applicantRepositoryProvider);
  }

  @Override
  protected String getProviderName() {
    return "";
  }

  @Override
  protected String getResponseMode() {
    return "form_post";
  }

  @Override
  protected String[] getExtraScopes() {
    return new String[] {};
  }

  @Override
  protected String attributePrefix() {
    return attributePrefix;
  }
  ;

  @Override
  protected String getClientSecret() {
    return getConfigurationValue(attributePrefix() + ".secret");
  }

  @Override
  public ProfileCreator getProfileAdapter(OidcConfiguration config, OidcClient client) {
    return new IdcsProfileAdapter(config, client, profileFactory, applicantRepositoryProvider);
  }
}
