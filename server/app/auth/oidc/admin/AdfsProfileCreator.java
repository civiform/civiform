package auth.oidc.admin;

import auth.CiviFormProfile;
import auth.IdentityProviderType;
import auth.Role;
import auth.oidc.CiviformOidcProfileCreator;
import auth.oidc.OidcClientProviderParams;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;

/**
 * This class takes an existing CiviForm profile and augments it with the information from an AD
 * profile. Right now this is only extracting the email address, since that is all that AD provides
 * right now.
 */
public class AdfsProfileCreator extends CiviformOidcProfileCreator {
  private final String adminGroupName;
  private final String adGroupsAttributeName;

  public AdfsProfileCreator(
      OidcConfiguration oidcConfiguration, OidcClient client, OidcClientProviderParams params) {
    super(oidcConfiguration, client, params);
    this.adminGroupName = params.configuration().getString("adfs.admin_group");
    this.adGroupsAttributeName = params.configuration().getString("adfs.ad_groups_attribute_name");
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
    List<String> groups = AdfsGroupAccessor.getGroups(profile, this.adGroupsAttributeName);
    return groups.contains(this.adminGroupName);
  }

  @Override
  public CiviFormProfile createEmptyCiviFormProfile(OidcProfile profile) {
    if (this.isGlobalAdmin(profile)) {
      return profileFactory.wrapProfileData(profileFactory.createNewAdmin());
    }
    return profileFactory.wrapProfileData(profileFactory.createNewProgramAdmin());
  }

  @Override
  protected IdentityProviderType identityProviderType() {
    return IdentityProviderType.ADMIN_IDENTITY_PROVIDER;
  }
}
