package auth.oidc.applicant;

import auth.ProfileFactory;
import auth.oidc.OidcClientProvider;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.util.Optional;
import javax.inject.Provider;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import repository.UserRepository;

public class GenericOidcClientProvider extends OidcClientProvider {

  private static final String ATTRIBUTE_PREFIX = "applicant_generic_oidc";
  private static final ImmutableList<String> DEFAULT_SCOPES =
      ImmutableList.of("openid", "profile", "email");

  private static final String PROVIDER_NAME_CONFIG_NAME = "provider_name";
  private static final String CLIENT_ID_CONFIG_NAME = "client_id";
  private static final String CLIENT_SECRET_CONFIG_NAME = "client_secret";
  private static final String DISCOVERY_URI_CONFIG_NAME = "discovery_uri";
  private static final String RESPONSE_MODE_CONFIG_NAME = "response_mode";
  private static final String RESPONSE_TYPE_CONFIG_NAME = "response_type";
  private static final String EXTRA_SCOPES_CONFIG_NAME = "additional_scopes";

  private static final String FIRST_NAME_ATTRIBUTE_CONFIG_NAME = "first_name_attribute";
  private static final String MIDDLE_NAME_ATTRIBUTE_CONFIG_NAME = "middle_name_attribute";
  private static final String LAST_NAME_ATTRIBUTE_CONFIG_NAME = "last_name_attribute";
  private static final String EMAIL_ATTRIBUTE_CONFIG_NAME = "email_attribute";
  private static final String LOCALE_ATTRIBUTE_CONFIG_NAME = "locale_attribute";

  @Inject
  public GenericOidcClientProvider(
      Config configuration,
      ProfileFactory profileFactory,
      Provider<UserRepository> applicantRepositoryProvider) {
    super(configuration, profileFactory, applicantRepositoryProvider);
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
    String emailAttr = getConfigurationValueOrThrow(EMAIL_ATTRIBUTE_CONFIG_NAME);
    Optional<String> localeAttr = getConfigurationValue(LOCALE_ATTRIBUTE_CONFIG_NAME);

    var nameAttrsBuilder = ImmutableList.<String>builder();
    getConfigurationValue(FIRST_NAME_ATTRIBUTE_CONFIG_NAME).ifPresent(nameAttrsBuilder::add);
    getConfigurationValue(MIDDLE_NAME_ATTRIBUTE_CONFIG_NAME).ifPresent(nameAttrsBuilder::add);
    getConfigurationValue(LAST_NAME_ATTRIBUTE_CONFIG_NAME).ifPresent(nameAttrsBuilder::add);
    return new GenericApplicantProfileCreator(
        config,
        client,
        profileFactory,
        accountRepositoryProvider,
        emailAttr,
        localeAttr.orElse(null),
        nameAttrsBuilder.build());
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
    if (extraScopesMaybe.isEmpty()) {
      return ImmutableList.of();
    }
    return ImmutableList.copyOf(extraScopesMaybe.get().split(" "));
  }

  @Override
  protected boolean getUseCsrf() {
    // In the future, we may wish to make this configurable, since this is a generic provider.
    return false;
  }
}
