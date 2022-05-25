package auth.oidc;

import auth.CiviFormProfile;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.UserRepository;

/**
 * This class takes an existing CiviForm profile and augments it with the information from an AD
 * profile. Right now this is only extracting the email address, since that is all that AD provides
 * right now.
 */
public class AdfsProfileAdapter extends OidcCiviFormProfileAdapter {
  private static final Logger logger = LoggerFactory.getLogger(AdfsProfileAdapter.class);

  private final String adminGroupName;
  private final String ad_groups_attribute_name;

  public AdfsProfileAdapter(
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
    JSONArray groups = (JSONArray) null;
    if (profile.containsAttribute(this.ad_groups_attribute_name)) {
      groups = profile.getAttribute(this.ad_groups_attribute_name, JSONArray.class);
    }

    if (groups == null) {
      logger.error("Missing group claim in ADFS OIDC profile.");
      return false;
    }

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
  protected void possiblyModifyConfigBasedOnCred(Credentials cred) {
    // No need!
  }
}
