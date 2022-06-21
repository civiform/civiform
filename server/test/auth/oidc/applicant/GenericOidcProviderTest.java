package auth.oidc.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProfileFactory;
import com.google.common.collect.ImmutableList;
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
import repository.ResetPostgres;
import repository.UserRepository;
import support.CfTestHelpers;
import java.util.Optional;

@RunWith(JUnitParamsRunner.class)
public class GenericOidcProviderTest extends ResetPostgres {
  private GenericOidcProvider genericOidcProvider;
  private ProfileFactory profileFactory;
  private static UserRepository userRepository;
  private static String DISCOVERY_URI = "http://oidc:3380/.well-known/openid-configuration";
  private static String BASE_URL = String.format("http://localhost:%d", Helpers.testServerPort());

  @Before
  public void setup() {
    userRepository = instanceOf(UserRepository.class);
    profileFactory = instanceOf(ProfileFactory.class);
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.<String, String>builder()
                .put("generic_oidc.provider_name", "Auth0")
                .put("generic_oidc.client_id", "civi")
                .put("generic_oidc.client_secret", "pass")
                .put("generic_oidc.response_mode", "form_post")
                .put("generic_oidc.response_type", "id_token")
                .put("generic_oidc.additional_scopes", "group")
                .put("generic_oidc.locale_attribute", "country")
                .put("generic_oidc.email_attribute", "email")
                .put("generic_oidc.first_name_attribute", "first")
                .put("generic_oidc.middle_name_attribute", "middle")
                .put("generic_oidc.last_name_attribute", "last")
                .put("generic_oidc.discovery_uri", DISCOVERY_URI)
                .put("base_url", BASE_URL)
                .build());

    // Just need some complete adaptor to access methods.
    genericOidcProvider =
        new GenericOidcProvider(
            config, profileFactory, CfTestHelpers.userRepositoryProvider(userRepository));
  }

  @Test
  public void Test_getConfigurationValues() {
    OidcClient client = genericOidcProvider.get();
    OidcConfiguration client_config = client.getConfiguration();
    ProfileCreator adaptor = genericOidcProvider.getProfileAdapter(client_config, client);

    assertThat(adaptor.getClass()).isEqualTo(GenericOidcProfileAdapter.class);
    GenericOidcProfileAdapter profileAdapter = (GenericOidcProfileAdapter) adaptor;

    String provider = genericOidcProvider.getProviderName().orElse("");
    assertThat(provider).isEqualTo("Auth0");

    String clientId = genericOidcProvider.getClientID();
    assertThat(clientId).isEqualTo("civi");

    String clientSecret = genericOidcProvider.getClientSecret();
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
    assertThat(profileAdapter.nameAttributeNames).isEqualTo(ImmutableList.of("first", "middle", "last"));
  }
}
