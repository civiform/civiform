package auth.oidc.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequestNew;

import auth.CiviFormProfileData;
import auth.IdentityProviderType;
import auth.ProfileFactory;
import auth.oidc.IdTokensFactory;
import auth.oidc.OidcClientProviderParams;
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
import org.pac4j.play.PlayWebContext;
import repository.AccountRepository;
import repository.ResetPostgres;
import support.CfTestHelpers;

public class GenericOidcProfileCreatorTest extends ResetPostgres {
  private GenericOidcProfileCreator genericOidcProfileCreator;
  private ProfileFactory profileFactory;
  private IdTokensFactory idTokensFactory;
  private static AccountRepository accountRepository;

  @Before
  public void setup() {
    accountRepository = instanceOf(AccountRepository.class);
    profileFactory = instanceOf(ProfileFactory.class);
    idTokensFactory = instanceOf(IdTokensFactory.class);
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
            OidcClientProviderParams.create(
                serverConfig,
                profileFactory,
                idTokensFactory,
                CfTestHelpers.userRepositoryProvider(accountRepository)));
  }

  @Test
  public void mergeCiviFormProfile_adminSucceeds() {
    OidcProfile profile = new OidcProfile();
    profile.addAttribute("email", "email@example.com");
    profile.addAttribute("groups", ImmutableList.of("CIVIFORM_GLOBAL_ADMIN"));
    // Required for OIDC profiles.
    profile.addAttribute("iss", "issuer");
    profile.setId("subject");

    PlayWebContext context = new PlayWebContext(fakeRequestNew());
    CiviFormProfileData profileData =
        genericOidcProfileCreator.mergeCiviFormProfile(Optional.empty(), profile, context);

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

    PlayWebContext context = new PlayWebContext(fakeRequestNew());
    CiviFormProfileData profileData =
        genericOidcProfileCreator.mergeCiviFormProfile(Optional.empty(), profile, context);

    assertThat(profileData.getRoles()).doesNotContain("ROLE_CIVIFORM_ADMIN");
  }

  @Test
  public void mergeCiviFormProfile_noGroupsInProfileDoesNotThrow() {
    OidcProfile profile = new OidcProfile();
    profile.addAttribute("email", "email@example.com");
    profile.removeAttribute("groups");
    // Required for OIDC profiles.
    profile.addAttribute("iss", "issuer");
    profile.setId("subject");

    PlayWebContext context = new PlayWebContext(fakeRequestNew());
    CiviFormProfileData profileData =
        genericOidcProfileCreator.mergeCiviFormProfile(Optional.empty(), profile, context);

    assertThat(profileData.getRoles()).doesNotContain("ROLE_CIVIFORM_ADMIN");
  }

  @Test
  public void genericOidcProfileCreator_identityProviderTypeIsCorrect() {
    assertThat(genericOidcProfileCreator.identityProviderType())
        .isEqualTo(IdentityProviderType.ADMIN_IDENTITY_PROVIDER);
  }
}
