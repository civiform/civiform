package auth.oidc.applicant;

import auth.ProfileFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.typesafe.config.Config;
import java.util.List;
import java.util.Optional;
import javax.inject.Provider;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.core.util.generator.RandomValueGenerator;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import repository.UserRepository;

/*
 * Login.gov (https://developers.login.gov/oidc/) OIDC provider using the PKCE method.
 */
public final class LoginGovClientProvider extends GenericOidcClientProvider {
  // Login.gov requires a state longer than 22 characters
  static final RandomValueGenerator stateGenerator = new RandomValueGenerator(30);

  @Inject
  public LoginGovClientProvider(
      Config configuration,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    super(configuration, profileFactory, applicantRepositoryProvider);
  }

  @Override
  @VisibleForTesting
  public String attributePrefix() {
    return "login_gov";
  }

  @Override
  public ProfileCreator getProfileCreator(OidcConfiguration config, OidcClient client) {

    var nameAttrs = ImmutableList.of("given_name", "family_name");
    return new GenericApplicantProfileCreator(
        config,
        client,
        profileFactory,
        applicantRepositoryProvider,
        "email",
        /*localeAttributeName*/ null,
        nameAttrs);
  }

  @Override
  protected String getResponseMode() {
    return "query";
  }

  @Override
  protected String getResponseType() {
    return "code";
  }

  @Override
  protected Optional<String> getProviderName() {
    return Optional.of("LoginGov");
  }

  @Override
  protected Optional<String> getClientSecret() {
    // No client secret used, since they can be insecure and is not requried by this flow.
    // Instead uses the PKCE method as required by login.gov.
    return Optional.empty();
  }

  @VisibleForTesting
  public String getACRValue() {
    return getConfigurationValue("acr_value").orElse("http://idmanagement.gov/ns/assurance/ial/1");
  }

  @Override
  public OidcConfiguration getConfig() {
    OidcConfiguration config = super.getConfig();
    config.setWithState(true);
    config.setStateGenerator(stateGenerator);
    config.addCustomParam("acr_values", getACRValue());
    config.addCustomParam("prompt", "select_account");
    config.setDisablePkce(false);
    config.setPkceMethod(CodeChallengeMethod.S256);
    return config;
  }

  @Override
  public OidcClient get() {
    OidcClient client = super.get();
    var providerMetadata = client.getConfiguration().getProviderMetadata();
    providerMetadata.setCodeChallengeMethods(List.of(CodeChallengeMethod.S256));

    return client;
  }
}
