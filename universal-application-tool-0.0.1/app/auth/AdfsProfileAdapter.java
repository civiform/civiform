package auth;

import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;

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
    uatProfile.setEmailAddress(oidcProfile.getAttribute("email", String.class)).join();
    return uatProfile.getProfileData();
  }
}
