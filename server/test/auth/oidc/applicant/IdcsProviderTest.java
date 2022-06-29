package auth.oidc.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProfileFactory;
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

@RunWith(JUnitParamsRunner.class)
public class IdcsProviderTest extends ResetPostgres {
  private IdcsProvider idcsProvider;
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
            ImmutableMap.of(
                "idcs.client_id",
                "foo",
                "idcs.secret",
                "bar",
                "idcs.discovery_uri",
                DISCOVERY_URI,
                "base_url",
                BASE_URL));

    // Just need some complete adaptor to access methods.
    idcsProvider =
        new IdcsProvider(
            config, profileFactory, CfTestHelpers.userRepositoryProvider(userRepository));
  }

  @Test
  public void Test_getConfigurationValues() {
    OidcClient client = idcsProvider.get();
    OidcConfiguration client_config = client.getConfiguration();
    ProfileCreator adaptor = idcsProvider.getProfileAdapter(client_config, client);

    String clientId = idcsProvider.getClientID();
    assertThat(clientId).isEqualTo("foo");

    String clientSecret = idcsProvider.getClientSecret();
    assertThat(clientSecret).isEqualTo("bar");

    String discoveryUri = idcsProvider.getDiscoveryURI();
    assertThat(discoveryUri).isEqualTo(DISCOVERY_URI);

    String responseType = idcsProvider.getResponseType();
    assertThat(responseType).isEqualTo("id_token");

    String callbackUrl = client.getCallbackUrl();
    assertThat(callbackUrl).isEqualTo(BASE_URL + "/callback");

    assertThat(adaptor.getClass()).isEqualTo(IdcsProfileAdapter.class);
  }
}
