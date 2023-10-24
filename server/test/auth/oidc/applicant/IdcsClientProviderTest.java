package auth.oidc.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProfileFactory;
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
public class IdcsClientProviderTest extends ResetPostgres {
  private IdcsClientProvider idcsProvider;
  private ProfileFactory profileFactory;
  private static AccountRepository accountRepository;
  private static final String DISCOVERY_URI =
      "http://dev-oidc:3390/.well-known/openid-configuration";
  private static final String BASE_URL =
      String.format("http://localhost:%d", Helpers.testServerPort());

  @Before
  public void setup() {
    accountRepository = instanceOf(AccountRepository.class);
    profileFactory = instanceOf(ProfileFactory.class);
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.of(
                "idcs.client_id",
                "idcs-fake-oidc-client",
                "idcs.secret",
                "idcs-fake-oidc-secret",
                "idcs.discovery_uri",
                DISCOVERY_URI,
                "base_url",
                BASE_URL));

    // Just need some complete adaptor to access methods.
    idcsProvider =
        new IdcsClientProvider(
            OidcClientProviderParams.create(
                config, profileFactory, CfTestHelpers.userRepositoryProvider(accountRepository)));
  }

  @Test
  public void Test_getConfigurationValues() {
    OidcClient client = idcsProvider.get();
    OidcConfiguration client_config = client.getConfiguration();
    ProfileCreator adaptor = idcsProvider.getProfileCreator(client_config, client);

    String clientId = idcsProvider.getClientID();
    assertThat(clientId).isEqualTo("idcs-fake-oidc-client");

    String clientSecret = idcsProvider.getClientSecret().get();
    assertThat(clientSecret).isEqualTo("idcs-fake-oidc-secret");

    String discoveryUri = idcsProvider.getDiscoveryURI();
    assertThat(discoveryUri).isEqualTo(DISCOVERY_URI);

    String responseType = idcsProvider.getResponseType();
    assertThat(responseType).isEqualTo("id_token");

    String callbackUrl = client.getCallbackUrl();
    assertThat(callbackUrl).isEqualTo(BASE_URL + "/callback");

    assertThat(adaptor.getClass()).isEqualTo(IdcsApplicantProfileCreator.class);
  }
}
