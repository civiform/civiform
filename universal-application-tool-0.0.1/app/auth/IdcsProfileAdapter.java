package auth;

import javax.inject.Provider;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import repository.ApplicantRepository;

/**
 * This class takes an existing UAT profile and augments it with the information from an IDCS
 * profile. Right now this is only extracting the email address, as a proof that this works - it
 * needs to be built out.
 * TODO(https://github.com/seattle-uat/universal-application-tool/issues/384): extract what's
 * possible.
 */
public class IdcsProfileAdapter extends UatProfileAdapter {

  public IdcsProfileAdapter(
      OidcConfiguration configuration,
      OidcClient client,
      ProfileFactory profileFactory,
      Provider<ApplicantRepository> applicantRepositoryProvider) {
    super(configuration, client, profileFactory, applicantRepositoryProvider);
  }

  @Override
  protected String emailAttributeName() {
    return "user_emailid";
  }

  @Override
  public UatProfileData uatProfileFromOidcProfile(OidcProfile profile) {
    return mergeUatProfile(
        profileFactory.wrapProfileData(profileFactory.createNewApplicant()), profile);
  }
}
