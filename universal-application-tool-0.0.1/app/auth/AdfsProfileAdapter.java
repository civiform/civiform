package auth;

import javax.inject.Provider;
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import repository.ApplicantRepository;

/**
 * This class takes an existing UAT profile and augments it with the information from an AD profile.
 * Right now this is only extracting the email address, as a proof that this works - it needs to be
 * built out. TODO(https://github.com/seattle-uat/universal-application-tool/issues/384): extract
 * what's possible.
 */
public class AdfsProfileAdapter extends UatProfileAdapter {

  public AdfsProfileAdapter(
      OidcConfiguration configuration,
      OidcClient client,
      ProfileFactory profileFactory,
      Provider<ApplicantRepository> applicantRepositoryProvider) {
    super(configuration, client, profileFactory, applicantRepositoryProvider);
  }

  @Override
  public UatProfileData uatProfileFromOidcProfile(OidcProfile profile) {
    return mergeUatProfile(
        profileFactory.wrapProfileData(profileFactory.createNewAdmin()), profile);
  }

  @Override
  public UatProfileData mergeUatProfile(UatProfile uatProfile, OidcProfile oidcProfile) {
    // The key in AD is just "email".
    String emailAddress = oidcProfile.getAttribute("email", String.class);
    uatProfile.setEmailAddress(emailAddress).join();
    uatProfile.getProfileData().addAttribute(CommonProfileDefinition.EMAIL, emailAddress);
    return uatProfile.getProfileData();
  }
}
