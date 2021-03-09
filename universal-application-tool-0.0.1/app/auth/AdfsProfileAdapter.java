package auth;

import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;

/**
 * This class takes an existing UAT profile and augments it with the information from an AD profile.
 * Right now this is only extracting the email address, as a proof that this works - it needs to be
 * built out. TODO(https://github.com/seattle-uat/universal-application-tool/issues/384): extract
 * what's possible.
 */
public class AdfsProfileAdapter extends UatProfileAdapter {

  public AdfsProfileAdapter(
      OidcConfiguration configuration, OidcClient client, ProfileFactory profileFactory) {
    super(configuration, client, profileFactory);
  }

  @Override
  public UatProfileData uatProfileFromOidcProfile(OidcProfile profile) {
    return mergeUatProfile(
        profileFactory.wrapProfileData(profileFactory.createNewAdmin()), profile);
  }

  @Override
  public UatProfileData mergeUatProfile(UatProfile uatProfile, OidcProfile oidcProfile) {
    // The key in AD is just "email".
    // TODO(https://github.com/seattle-uat/universal-application-tool/issues/385): what if there's
    // already an email?
    uatProfile.setEmailAddress(oidcProfile.getAttribute("email", String.class)).join();
    return uatProfile.getProfileData();
  }
}
