package auth.oidc.admin;

import auth.CiviFormProfile;
import auth.ProfileFactory;
import auth.Role;
import auth.oidc.CiviformOidcProfileCreator;
import auth.oidc.IdTokensFactory;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import java.util.List;
import javax.inject.Provider;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import repository.AccountRepository;

/**
 * This class creates a pac4j `UserProfile` for admins when the identity provider is a generic OIDC
 * provider.
 */
public class GenericOidcProfileCreator extends CiviformOidcProfileCreator {
  private String groupsAttributeName;
  private String adminGroupName;

  // This config variable holds the name of the profile attribute that lists the groups that apply
  // to the user.
  private static String ID_GROUPS_ATTRIBUTE_NAME = "admin_generic_oidc_id_groups_attribute_name";
  // This config variable holds the name of the group that is used to specify that a user is a
  // global admin.
  private static String ADMIN_GROUP_CONFIG_NAME = "admin_generic_oidc_admin_group_name";

  public GenericOidcProfileCreator(
      OidcConfiguration configuration,
      OidcClient client,
      ProfileFactory profileFactory,
      IdTokensFactory idTokensFactory,
      Config appConfig,
      Provider<AccountRepository> accountRepositoryProvider) {
    super(configuration, client, profileFactory, idTokensFactory, accountRepositoryProvider);
    this.groupsAttributeName = appConfig.getString(ID_GROUPS_ATTRIBUTE_NAME);
    this.adminGroupName = appConfig.getString(ADMIN_GROUP_CONFIG_NAME);
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
    if (isTrustedIntermediary(profile)) {
      // Give ROLE_APPLICANT in addition to ROLE_TI so that the TI can perform applicant actions.
      return ImmutableSet.of(Role.ROLE_APPLICANT, Role.ROLE_TI);
    }
    return ImmutableSet.of(Role.ROLE_PROGRAM_ADMIN);
  }

  private boolean isGlobalAdmin(OidcProfile profile) {
    @SuppressWarnings("unchecked")
    List<String> groups = (List) profile.getAttribute(this.groupsAttributeName);
    return groups.contains(this.adminGroupName);
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

  @Override
  public CiviFormProfile createEmptyCiviFormProfile(OidcProfile profile) {
    if (this.isGlobalAdmin(profile)) {
      return profileFactory.wrapProfileData(profileFactory.createNewAdmin());
    }
    return profileFactory.wrapProfileData(profileFactory.createNewProgramAdmin());
  }
}
