package auth.oidc.admin;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProfileFactory;
import auth.oidc.IdTokensFactory;
import auth.oidc.OidcClientProviderParams;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
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
  private static AccountRepository accountProvider;
  private static final String DISCOVERY_URI =
      "http://dev-oidc:3390/.well-known/openid-configuration";
  private static final String BASE_URL =
      String.format("http://localhost:%d", Helpers.testServerPort());

  @Before
  public void setup() {
    accountProvider = instanceOf(AccountRepository.class);
    profileFactory = instanceOf(ProfileFactory.class);
    idTokensFactory = instanceOf(IdTokensFactory.class);
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.<String, String>builder()
                .put("admin_generic_oidc_provider_name", "Okta")
                .put("admin_generic_oidc_client_id", "civi")
                .put("admin_generic_oidc_client_secret", "pass")
                .put("admin_generic_oidc_response_mode", "form_post")
                .put("admin_generic_oidc_response_type", "id_token")
                .put("admin_generic_oidc_additional_scopes", "group")
                .put("admin_generic_oidc_discovery_uri", DISCOVERY_URI)
                .put("admin_generic_oidc_id_groups_attribute_name", "groups")
                .put("admin_generic_oidc_admin_group_name", "admin group")
                .put("admin_generic_oidc_use_csrf", "true")
                .put("base_url", BASE_URL)
                .build());

    // Just need some complete adaptor to access methods.
    genericOidcProvider =
        new GenericOidcClientProvider(
            OidcClientProviderParams.create(
                config,
                profileFactory,
                idTokensFactory,
                CfTestHelpers.userRepositoryProvider(accountProvider)));
  }

  @Test
  public void Test_getConfigurationValues() {
    OidcClient client = genericOidcProvider.get();
    OidcConfiguration client_config = client.getConfiguration();

    ProfileCreator adaptor = genericOidcProvider.getProfileCreator(client_config, client);
    assertThat(adaptor.getClass()).isEqualTo(GenericOidcProfileCreator.class);

    String provider = genericOidcProvider.getProviderName().orElse("");
    assertThat(provider).isEqualTo("Okta");

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

    boolean useCsrf = genericOidcProvider.getUseCsrf();
    assertThat(useCsrf).isTrue();
  }
}
