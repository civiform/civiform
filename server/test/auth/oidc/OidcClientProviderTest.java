package auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import auth.ProfileFactory;
import auth.oidc.applicant.IdcsApplicantProfileCreator;
import auth.oidc.applicant.IdcsClientProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
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
public class OidcClientProviderTest extends ResetPostgres {
  private OidcClientProvider oidcClientProvider;
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
    oidcClientProvider =
        new IdcsClientProvider(
            OidcClientProviderParams.create(
                config,
                profileFactory,
                idTokensFactory,
                CfTestHelpers.userRepositoryProvider(accountRepository)));
  }

  @Test
  public void Test_getConfigurationValues() {
    String client_id = oidcClientProvider.getClientID();
    assertThat(client_id).isEqualTo("idcs-fake-oidc-client");

    String client_secret = oidcClientProvider.getClientSecret().get();
    assertThat(client_secret).isEqualTo("idcs-fake-oidc-secret");
  }

  static ImmutableList<Object[]> provideConfigsForGet() {
    return ImmutableList.of(
        new Object[] {
          "normal",
          "id_token",
          ImmutableMap.of(
              "idcs.client_id",
              "idcs-fake-oidc-client",
              "idcs.secret",
              "idcs-fake-oidc-secret",
              "idcs.discovery_uri",
              DISCOVERY_URI,
              "base_url",
              BASE_URL)
        },
        new Object[] {
          "extra args that aren't used",
          "id_token",
          ImmutableMap.of(
              "idcs.client_id",
              "idcs-fake-oidc-client",
              "idcs.secret",
              "idcs-fake-oidc-secret",
              "idcs.provider_name",
              "Provider Name here",
              "idcs.response_mode",
              "Try to override",
              "idcs.additional_scopes",
              "No more scopes",
              "idcs.discovery_uri",
              DISCOVERY_URI,
              "base_url",
              BASE_URL)
        });
  }

  @Test
  @TestCaseName("{index} {0} config get() should be parsable")
  @Parameters(method = "provideConfigsForGet")
  public void Test_get(String name, String wantResponseType, ImmutableMap<String, String> c) {
    Config config = ConfigFactory.parseMap(c);

    OidcClientProvider oidcClientProvider =
        new IdcsClientProvider(
            OidcClientProviderParams.create(
                config,
                profileFactory,
                idTokensFactory,
                CfTestHelpers.userRepositoryProvider(accountRepository)));

    OidcClient client = oidcClientProvider.get();

    assertThat(client.getCallbackUrl()).isEqualTo(c.get("base_url") + "/callback");
    assertThat(client.getName()).isEqualTo("OidcClient");

    OidcConfiguration client_config = client.getConfiguration();

    assertThat(client_config.getClientId()).isEqualTo(c.get("idcs.client_id"));
    assertThat(client_config.getSecret()).isEqualTo(c.get("idcs.secret"));
    assertThat(client_config.getDiscoveryURI()).isEqualTo(c.get("idcs.discovery_uri"));
    assertThat(client_config.getScope()).isEqualTo("openid profile email");
    assertThat(client_config.getResponseType()).isEqualTo(wantResponseType);
    assertThat(client_config.getResponseMode()).isEqualTo("form_post");

    ProfileCreator adaptor = client.getProfileCreator();

    assertThat(adaptor.getClass()).isEqualTo(IdcsApplicantProfileCreator.class);
  }

  static ImmutableList<Object[]> provideConfigsForInvalidConfig() {
    return ImmutableList.of(
        new Object[] {
          "blank client_id",
          ImmutableMap.of(
              "idcs.client_id",
              "",
              "idcs.secret",
              "idcs-fake-oidc-secret",
              "idcs.discovery_uri",
              DISCOVERY_URI,
              "base_url",
              BASE_URL)
        },
        new Object[] {
          "missing base_url",
          ImmutableMap.of(
              "idcs.client_id",
              "idcs-fake-oidc-client",
              "idcs.secret",
              "idcs-fake-oidc-secret",
              "idcs.discovery_uri",
              DISCOVERY_URI)
        },
        new Object[] {
          "blank discovery uri",
          ImmutableMap.of(
              "idcs.client_id",
              "idcs-fake-oidc-client",
              "idcs.secret",
              "idcs-fake-oidc-secret",
              "idcs.discovery_uri",
              "",
              "base_url",
              BASE_URL)
        });
  }

  @Test
  @TestCaseName("{index} {0} config get() should return null config")
  @Parameters(method = "provideConfigsForInvalidConfig")
  public void get_invalidConfig(String name, ImmutableMap<String, String> c) {
    Config bad_secret_config = ConfigFactory.parseMap(c);
    assertThatThrownBy(
            () -> {
              OidcClientProvider badOidcClientProvider =
                  new IdcsClientProvider(
                      OidcClientProviderParams.create(
                          bad_secret_config,
                          profileFactory,
                          idTokensFactory,
                          CfTestHelpers.userRepositoryProvider(accountRepository)));
              badOidcClientProvider.get();
            })
        .isInstanceOf(RuntimeException.class);
  }
}
