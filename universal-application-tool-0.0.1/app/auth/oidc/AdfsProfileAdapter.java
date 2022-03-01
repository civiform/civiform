package auth.oidc;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.Roles;
import com.google.common.collect.ImmutableSet;
import com.nimbusds.jose.shaded.json.JSONArray;
import com.typesafe.config.Config;
import javax.inject.Provider;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import repository.UserRepository;

/**
 * This class takes an existing CiviForm profile and augments it with the information from an AD
 * profile. Right now this is only extracting the email address, since that is all that AD provides
 * right now.
 */
public class AdfsProfileAdapter extends OidcCiviFormProfileAdapter {
  private final String adminGroupName;

  public AdfsProfileAdapter(
      OidcConfiguration configuration,
      OidcClient client,
      ProfileFactory profileFactory,
      Config appConfig,
      Provider<UserRepository> applicantRepositoryProvider) {
    super(configuration, client, profileFactory, applicantRepositoryProvider);
    this.adminGroupName = appConfig.getString("adfs.admin_group");
  }

  @Override
  protected String emailAttributeName() {
    return "email";
  }

  @Override
  protected ImmutableSet<Roles> roles(CiviFormProfile profile, OidcProfile oidcProfile) {
    if (this.isGlobalAdmin(oidcProfile)) {
      return ImmutableSet.of(Roles.ROLE_CIVIFORM_ADMIN);
    }
    return ImmutableSet.of(Roles.ROLE_PROGRAM_ADMIN);
  }

  @Override
  protected void adaptForRole(CiviFormProfile profile, ImmutableSet<Roles> roles) {
    if (roles.contains(Roles.ROLE_CIVIFORM_ADMIN)) {
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
    JSONArray groups = profile.getAttribute("group", JSONArray.class);
    return groups.contains(this.adminGroupName);
  }

  @Override
  public CiviFormProfileData civiformProfileFromOidcProfile(OidcProfile profile) {
    if (this.isGlobalAdmin(profile)) {
      return mergeCiviFormProfile(
          profileFactory.wrapProfileData(profileFactory.createNewAdmin()), profile);
    }
    return mergeCiviFormProfile(
        profileFactory.wrapProfileData(profileFactory.createNewProgramAdmin()), profile);
  }

  @Override
  protected void possiblyModifyConfigBasedOnCred(Credentials cred) {
    // No need!
  }
}
