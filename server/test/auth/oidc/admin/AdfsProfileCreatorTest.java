package auth.oidc.admin;

import static org.assertj.core.api.Assertions.assertThat;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.Role;
import auth.oidc.OidcClientProviderParams;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import models.AccountModel;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import repository.AccountRepository;
import repository.ResetPostgres;
import repository.StoredFileRepository;
import support.CfTestHelpers;

public class AdfsProfileCreatorTest extends ResetPostgres {
  private AdfsProfileCreator adfsProfileCreator;
  private ProfileFactory profileFactory;

  @Before
  public void setup() {
    AccountRepository accountRepository = instanceOf(AccountRepository.class);
    profileFactory = instanceOf(ProfileFactory.class);
    var storedFileRepository = instanceOf(StoredFileRepository.class);
    OidcClient client = CfTestHelpers.getOidcClient("dev-oidc", 3390);
    OidcConfiguration client_config = CfTestHelpers.getOidcConfiguration("dev-oidc", 3390);
    Config serverConfig =
        ConfigFactory.parseMap(
            ImmutableMap.<String, String>builder()
                .put("adfs.ad_groups_attribute_name", "groups")
                .put("adfs.admin_group", "CIVIFORM_GLOBAL_ADMIN")
                .build());
    adfsProfileCreator =
        new AdfsProfileCreator(
            client_config,
            client,
            OidcClientProviderParams.create(
                serverConfig, profileFactory, () -> accountRepository, () -> storedFileRepository));
  }

  @Test
  public void adaptForRole_promotionPersistsGlobalAdmin() {
    CiviFormProfileData data = profileFactory.createNewProgramAdmin();
    CiviFormProfile profile = profileFactory.wrapProfileData(data);

    adfsProfileCreator.adaptForRole(profile, ImmutableSet.of(Role.ROLE_CIVIFORM_ADMIN));

    AccountModel account = profile.getAccount().join();
    account.refresh();
    assertThat(account.getGlobalAdmin()).isTrue();
  }

  @Test
  public void adaptForRole_demotionClearsGlobalAdmin() {
    CiviFormProfileData data = profileFactory.createNewAdmin();
    CiviFormProfile profile = profileFactory.wrapProfileData(data);

    adfsProfileCreator.adaptForRole(profile, ImmutableSet.of(Role.ROLE_PROGRAM_ADMIN));

    AccountModel account = profile.getAccount().join();
    account.refresh();
    assertThat(account.getGlobalAdmin()).isFalse();
  }
}
