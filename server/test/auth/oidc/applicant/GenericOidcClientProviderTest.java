package auth.oidc.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProfileFactory;
import auth.oidc.IdTokensFactory;
import auth.oidc.OidcClientProviderParams;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pac4j.core.profile.creator.ProfileCreator;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import play.api.test.Helpers;
import repository.AccountRepository;
import repository.ResetPostgres;
import support.CfTestHelpers;

@RunWith(JUnitParamsRunner.class)
public class GenericOidcClientProviderTest extends ResetPostgres {
  private GenericOidcClientProvider genericOidcProvider;
  private ProfileFactory profileFactory;
  private IdTokensFactory idTokensFactory;
  private static AccountRepository accountRepository;
  private static final String DISCOVERY_URI =
      "http://dev-oidc:3390/.well-known/openid-configuration";
  private static final String BASE_URL =
      String.format("http://localhost:%d", Helpers.testServerPort());

  @Before
  public void setup() {
    accountRepository = instanceOf(AccountRepository.class);
    profileFactory = instanceOf(ProfileFactory.class);
    idTokensFactory = instanceOf(IdTokensFactory.class);
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.<String, String>builder()
                .put("applicant_generic_oidc.provider_name", "Auth0")
                .put("applicant_generic_oidc.client_id", "civi")
                .put("applicant_generic_oidc.client_secret", "pass")
                .put("applicant_generic_oidc.response_mode", "form_post")
                .put("applicant_generic_oidc.response_type", "id_token")
                .put("applicant_generic_oidc.additional_scopes", "group")
                .put("applicant_generic_oidc.locale_attribute", "country")
                .put("applicant_generic_oidc.email_attribute", "email")
                .put("applicant_generic_oidc.first_name_attribute", "first")
                .put("applicant_generic_oidc.middle_name_attribute", "middle")
                .put("applicant_generic_oidc.last_name_attribute", "last")
                .put("applicant_generic_oidc.name_suffix_attribute", "I.")
                .put("applicant_generic_oidc.discovery_uri", DISCOVERY_URI)
                .put("base_url", BASE_URL)
                .build());

    // Just need some complete adaptor to access methods.
    genericOidcProvider =
        new GenericOidcClientProvider(
            OidcClientProviderParams.create(
                config,
                profileFactory,
                idTokensFactory,
                CfTestHelpers.userRepositoryProvider(accountRepository)));
  }

  @Test
  public void Test_getConfigurationValues() {
    OidcClient client = genericOidcProvider.get();
    OidcConfiguration client_config = client.getConfiguration();
    ProfileCreator adaptor = genericOidcProvider.getProfileCreator(client_config, client);

    assertThat(adaptor.getClass()).isEqualTo(GenericApplicantProfileCreator.class);
    GenericApplicantProfileCreator profileAdapter = (GenericApplicantProfileCreator) adaptor;

    String provider = genericOidcProvider.getProviderName().orElse("");
    assertThat(provider).isEqualTo("Auth0");

    String clientId = genericOidcProvider.getClientID();
    assertThat(clientId).isEqualTo("civi");

    String clientSecret = genericOidcProvider.getClientSecret().get();
    assertThat(clientSecret).isEqualTo("pass");

    String responseType = genericOidcProvider.getResponseType();
    assertThat(responseType).isEqualTo("id_token");

    String responseMode = genericOidcProvider.getResponseMode();
    assertThat(responseMode).isEqualTo("form_post");

    String scope = genericOidcProvider.getScopesAttribute();
    assertThat(scope).isEqualTo("openid profile email group");

    String discoveryUri = genericOidcProvider.getDiscoveryURI();
    assertThat(discoveryUri).isEqualTo(DISCOVERY_URI);

    String callbackUrl = client.getCallbackUrl();
    assertThat(callbackUrl).isEqualTo(BASE_URL + "/callback");

    assertThat(profileAdapter.emailAttributeName).isEqualTo("email");
    assertThat(profileAdapter.localeAttributeName).isEqualTo(Optional.of("country"));
    assertThat(profileAdapter.nameAttributeNames)
        .isEqualTo(ImmutableList.of("first", "middle", "last"));
  }
}
