package auth.oidc.admin;

import auth.CiviFormProfile;
import auth.ProfileFactory;
import auth.Role;
import auth.oidc.CiviformOidcProfileCreator;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import java.util.List;
import javax.inject.Provider;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import repository.UserRepository;

/**
 * This class takes an existing CiviForm profile and augments it with the information from an AD
 * profile. Right now this is only extracting the email address, since that is all that AD provides
 * right now.
 */
public class AdfsProfileCreator extends CiviformOidcProfileCreator {
  private final String adminGroupName;
  private final String ad_groups_attribute_name;

  public AdfsProfileCreator(
      OidcConfiguration configuration,
      OidcClient client,
      ProfileFactory profileFactory,
      Config appConfig,
      Provider<UserRepository> applicantRepositoryProvider) {
    super(configuration, client, profileFactory, applicantRepositoryProvider);
    this.adminGroupName = appConfig.getString("adfs.admin_group");
    this.ad_groups_attribute_name = appConfig.getString("adfs.ad_groups_attribute_name");
  }

  @Override
  protected String emailAttributeName() {
    return "email";
  }

  @Override
  protected ImmutableSet<Role> roles(CiviFormProfile profile, OidcProfile oidcProfile) {
    if (this.isGlobalAdmin(oidcProfile)) {
      return ImmutableSet.of(Role.ROLE_CIVIFORM_ADMIN);
    }
    return ImmutableSet.of(Role.ROLE_PROGRAM_ADMIN);
  }

  @Override
  protected void adaptForRole(CiviFormProfile profile, ImmutableSet<Role> roles) {
    if (roles.contains(Role.ROLE_CIVIFORM_ADMIN)) {
      profile
          .getAccount()
          .thenAccept(
              account -> {
                account.setGlobalAdmin(true);
                account.save();
              })
          .join();
    }
  }

  private boolean isGlobalAdmin(OidcProfile profile) {
    List<String> groups = AdfsGroupAccessor.getGroups(profile, this.ad_groups_attribute_name);
    return groups.contains(this.adminGroupName);
  }

  @Override
  public CiviFormProfile createEmptyCiviFormProfile(OidcProfile profile) {
    if (this.isGlobalAdmin(profile)) {
      return profileFactory.wrapProfileData(profileFactory.createNewAdmin());
    }
    return profileFactory.wrapProfileData(profileFactory.createNewProgramAdmin());
  }
}
