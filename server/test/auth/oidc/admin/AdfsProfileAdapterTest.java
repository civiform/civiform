package auth.oidc.admin;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProfileFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import repository.ResetPostgres;
import repository.UserRepository;
import support.CfTestHelpers;

public class AdfsProfileAdapterTest extends ResetPostgres {
  private AdfsProfileAdapter adfsProfileAdapter;
  private ProfileFactory profileFactory;
  private static UserRepository userRepository;

  @Before
  public void setup() {
    userRepository = instanceOf(UserRepository.class);
    profileFactory = instanceOf(ProfileFactory.class);
    OidcClient client = CfTestHelpers.getOidcClient("oidc", 3380);
    OidcConfiguration client_config = CfTestHelpers.getOidcConfiguration("oidc", 3380);
    Config appConfig =
        ConfigFactory.parseMap(
            ImmutableMap.of(
                "adfs.admin_group", "admin_group",
                "adfs.ad_groups_attribute_name", "admin_group_attribute"));
    adfsProfileAdapter =
        new AdfsProfileAdapter(
            client_config,
            client,
            profileFactory,
            appConfig,
            CfTestHelpers.userRepositoryProvider(userRepository));
  }

  @Test
  public void makeGlobalAdminProfile_success() {
    OidcProfile profile = new OidcProfile();
    profile.addAttribute("admin_group_attribute", ImmutableList.of("admin_group"));
    assertThat(adfsProfileAdapter.createEmptyCiviFormProfile(profile).isCiviFormAdmin()).isTrue();
  }

  @Test
  public void makeProfile_wrongGroupName() {
    OidcProfile profile = new OidcProfile();
    profile.addAttribute("admin_group_attribute", ImmutableList.of("not_admin_group"));
    assertThat(adfsProfileAdapter.createEmptyCiviFormProfile(profile).isCiviFormAdmin()).isFalse();
    assertThat(adfsProfileAdapter.createEmptyCiviFormProfile(profile).isProgramAdmin()).isTrue();
  }

  @Test
  public void makeProfile_nullAdminGroup() {
    OidcProfile profile = new OidcProfile();
    profile.addAttribute("admin_group_attribute", null);
    assertThat(adfsProfileAdapter.createEmptyCiviFormProfile(profile).isCiviFormAdmin()).isFalse();
    assertThat(adfsProfileAdapter.createEmptyCiviFormProfile(profile).isProgramAdmin()).isTrue();
  }

  @Test
  public void makeProgramAdminProfile_success() {
    OidcProfile profile = new OidcProfile();
    assertThat(adfsProfileAdapter.createEmptyCiviFormProfile(profile).isCiviFormAdmin()).isFalse();
    assertThat(adfsProfileAdapter.createEmptyCiviFormProfile(profile).isProgramAdmin()).isTrue();
  }
}
