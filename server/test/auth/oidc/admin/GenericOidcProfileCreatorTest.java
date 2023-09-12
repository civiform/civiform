package auth.oidc.admin;

import static org.assertj.core.api.Assertions.assertThat;

import auth.CiviFormProfileData;
import auth.ProfileFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import repository.ResetPostgres;
import repository.UserRepository;
import support.CfTestHelpers;

public class GenericOidcProfileCreatorTest extends ResetPostgres {
  private GenericOidcProfileCreator genericOidcProfileCreator;
  private ProfileFactory profileFactory;
  private static UserRepository accountRepository;

  @Before
  public void setup() {
    accountRepository = instanceOf(UserRepository.class);
    profileFactory = instanceOf(ProfileFactory.class);
    OidcClient client = CfTestHelpers.getOidcClient("dev-oidc", 3390);
    OidcConfiguration client_config = CfTestHelpers.getOidcConfiguration("dev-oidc", 3390);
    Config serverConfig =
        ConfigFactory.parseMap(
            ImmutableMap.<String, String>builder()
                .put("admin_generic_oidc_id_groups_attribute_name", "groups")
                .put("admin_generic_oidc_admin_group_name", "CIVIFORM_GLOBAL_ADMIN")
                .build());
    genericOidcProfileCreator =
        new GenericOidcProfileCreator(
            client_config,
            client,
            profileFactory,
            serverConfig,
            CfTestHelpers.userRepositoryProvider(accountRepository));
  }

  @Test
  public void mergeCiviFormProfile_adminSucceeds() {
    OidcProfile profile = new OidcProfile();
    profile.addAttribute("email", "email@example.com");
    profile.addAttribute("groups", ImmutableList.of("CIVIFORM_GLOBAL_ADMIN"));
    // Required for OIDC profiles.
    profile.addAttribute("iss", "issuer");
    profile.setId("subject");

    CiviFormProfileData profileData =
        genericOidcProfileCreator.mergeCiviFormProfile(Optional.empty(), profile);

    assertThat(profileData.getRoles()).contains("ROLE_CIVIFORM_ADMIN");
  }

  @Test
  public void mergeCiviFormProfile_nonAdminSucceeds() {
    OidcProfile profile = new OidcProfile();
    profile.addAttribute("email", "email@example.com");
    profile.addAttribute("groups", ImmutableList.of("NON_ADMIN_GROUP"));
    // Required for OIDC profiles.
    profile.addAttribute("iss", "issuer");
    profile.setId("subject");

    CiviFormProfileData profileData =
        genericOidcProfileCreator.mergeCiviFormProfile(Optional.empty(), profile);

    assertThat(profileData.getRoles()).doesNotContain("ROLE_CIVIFORM_ADMIN");
  }
}
