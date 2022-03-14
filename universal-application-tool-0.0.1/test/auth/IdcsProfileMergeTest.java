package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import auth.oidc.IdcsProfileAdapter;
import java.util.concurrent.ExecutionException;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.oidc.profile.OidcProfile;
import repository.UserRepository;
import repository.WithPostgresContainer;

public class IdcsProfileMergeTest extends WithPostgresContainer {

  private IdcsProfileAdapter profileAdapter;
  private ProfileFactory profileFactory;

  @Before
  public void setupFactory() {
    profileFactory = instanceOf(ProfileFactory.class);
    UserRepository repository = instanceOf(UserRepository.class);
    profileAdapter =
        new IdcsProfileAdapter(
            null,
            null,
            profileFactory,
            new Provider<UserRepository>() {
              @Override
              public UserRepository get() {
                return repository;
              }
            });
  }

  @Test
  public void testProfileCreation() throws ExecutionException, InterruptedException {
    OidcProfile oidcProfile = new OidcProfile();
    oidcProfile.addAttribute("user_emailid", "foo@example.com");
    // authority_id values.
    oidcProfile.addAttribute("iss", "issuer");
    oidcProfile.setId("subject");

    CiviFormProfileData profileData = profileAdapter.civiformProfileFromOidcProfile(oidcProfile);
    CiviFormProfile profile = profileFactory.wrapProfileData(profileData);

    assertThat(profileData.getEmail()).isEqualTo("foo@example.com");
    assertThat(profile.getEmailAddress().get()).isEqualTo("foo@example.com");
    assertThat(profile.getAuthorityId().get()).isEqualTo("iss: issuer sub: subject");
  }

  @Test
  public void testSuccessfulProfileMerge() {
    OidcProfile oidcProfile = new OidcProfile();
    oidcProfile.addAttribute("user_emailid", "foo@example.com");
    // authority_id values.
    oidcProfile.addAttribute("iss", "issuer");
    oidcProfile.setId("subject");

    CiviFormProfileData profileData = profileAdapter.civiformProfileFromOidcProfile(oidcProfile);

    assertThat(
            profileAdapter
                .mergeCiviFormProfile(profileFactory.wrapProfileData(profileData), oidcProfile)
                .getEmail())
        .isEqualTo("foo@example.com");
  }

  @Test
  public void testFailedProfileMerge() {
    OidcProfile oidcProfile = new OidcProfile();
    oidcProfile.addAttribute("user_emailid", "foo@example.com");
    OidcProfile conflictingProfile = new OidcProfile();
    oidcProfile.addAttribute("user_emailid", "bar@example.com");

    CiviFormProfileData profileData = profileAdapter.civiformProfileFromOidcProfile(oidcProfile);

    assertThatThrownBy(
            () ->
                profileAdapter.mergeCiviFormProfile(
                    profileFactory.wrapProfileData(profileData), conflictingProfile))
        .hasCauseInstanceOf(ProfileMergeConflictException.class);
  }
}
