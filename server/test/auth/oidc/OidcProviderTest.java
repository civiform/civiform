package auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProfileFactory;
import auth.oidc.applicant.IdcsProfileAdapter;
import auth.oidc.applicant.IdcsProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import javax.inject.Provider;
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
import repository.ResetPostgres;
import repository.UserRepository;

@RunWith(JUnitParamsRunner.class)
public class OidcProviderTest extends ResetPostgres {
  private OidcProvider oidcProvider;
  private ProfileFactory profileFactory;
  private static UserRepository userRepository;
  private static String discoveryUri = "http://oidc:3380/.well-known/openid-configuration";
  private static String baseUrl = String.format("http://localhost:%d", Helpers.testServerPort());

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
                discoveryUri,
                "base_url",
                baseUrl));

    // Just need some complete adaptor to access methods.
    oidcProvider =
        new IdcsProvider(
            config,
            profileFactory,
            new Provider<UserRepository>() {
              @Override
              public UserRepository get() {
                return userRepository;
              }
            });
  }

  @Test
  public void Test_getConfigurationValues() {
    String client_id = oidcProvider.getClientID();
    assertThat(client_id).isEqualTo("foo");

    String client_secret = oidcProvider.getClientSecret();
    assertThat(client_secret).isEqualTo("bar");
  }

  static ImmutableList<Object[]> provideConfigsForGet() {
    return ImmutableList.of(
        new Object[] {
          "normal",
          "id_token",
          ImmutableMap.of(
              "idcs.client_id",
              "foo",
              "idcs.secret",
              "bar",
              "idcs.discovery_uri",
              discoveryUri,
              "base_url",
              baseUrl)
        },
        new Object[] {
          "extra args that aren't used",
          "id_token",
          ImmutableMap.of(
              "idcs.client_id", "foo",
              "idcs.secret", "bar",
              "idcs.provider_name", "Provider Name here",
              "idcs.response_mode", "Try to override",
              "idcs.additional_scopes", "No more scopes",
              "idcs.discovery_uri", discoveryUri,
              "base_url", baseUrl)
        });
  }

  @Test
  @TestCaseName("{index} {0} config get() should be parsable")
  @Parameters(method = "provideConfigsForGet")
  public void Test_get(String name, String wantResponseType, ImmutableMap<String, String> c) {
    Config config = ConfigFactory.parseMap(c);

    OidcProvider oidcProvider =
        new IdcsProvider(
            config,
            profileFactory,
            new Provider<UserRepository>() {
              @Override
              public UserRepository get() {
                return userRepository;
              }
            });
    OidcClient client = oidcProvider.get();

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

    assertThat(adaptor.getClass()).isEqualTo(IdcsProfileAdapter.class);
  }

  static ImmutableList<Object[]> provideConfigsForInvalidConfig() {
    return ImmutableList.of(
        new Object[] {
          "blank client_id",
          ImmutableMap.of(
              "idcs.client_id",
              "",
              "idcs.secret",
              "bar",
              "idcs.discovery_uri",
              discoveryUri,
              "base_url",
              baseUrl)
        },
        new Object[] {
          "blank secret",
          ImmutableMap.of(
              "idcs.client_id",
              "foo",
              "idcs.secret",
              "",
              "idcs.discovery_uri",
              discoveryUri,
              "base_url",
              baseUrl)
        },
        new Object[] {
          "missing secret",
          ImmutableMap.of(
              "idcs.client_id", "foo",
              "idcs.discovery_uri", discoveryUri,
              "base_url", baseUrl)
        },
        new Object[] {
          "missing base_url",
          ImmutableMap.of(
              "idcs.client_id", "foo",
              "idcs.secret", "bar",
              "idcs.discovery_uri", discoveryUri)
        },
        new Object[] {
          "blank discovery uri",
          ImmutableMap.of(
              "idcs.client_id", "foo",
              "idcs.secret", "bar",
              "idcs.discovery_uri", "",
              "base_url", baseUrl)
        });
  }

  @Test
  @TestCaseName("{index} {0} config get() should return null config")
  @Parameters(method = "provideConfigsForInvalidConfig")
  public void get_invalidConfig(String name, ImmutableMap<String, String> c) {
    Config empty_secret_config = ConfigFactory.parseMap(c);

    OidcProvider badOidcProvider =
        new IdcsProvider(
            empty_secret_config,
            profileFactory,
            new Provider<UserRepository>() {
              @Override
              public UserRepository get() {
                return userRepository;
              }
            });
    OidcClient client = badOidcProvider.get();
    assertThat(client).isNull();
  }
}
