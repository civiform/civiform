package auth;

import com.google.common.collect.ImmutableSet;
import javax.inject.Provider;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import repository.ApplicantRepository;

/**
 * This class takes an existing UAT profile and augments it with the information from an AD profile.
 * Right now this is only extracting the email address, since that is all that AD provides right
 * now.
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
  protected String emailAttributeName() {
    return "email";
  }

  @Override
  protected ImmutableSet<Roles> roles(UatProfile profile) {
    return ImmutableSet.of(Roles.ROLE_UAT_ADMIN);
  }

  @Override
  public UatProfileData uatProfileFromOidcProfile(OidcProfile profile) {
    return mergeUatProfile(
        profileFactory.wrapProfileData(profileFactory.createNewAdmin()), profile);
  }
}
