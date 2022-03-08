package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import auth.oidc.IdcsProfileAdapter;
import auth.saml.SamlCiviFormProfileAdapter;
import java.util.concurrent.ExecutionException;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.saml.profile.SAML2Profile;
import repository.UserRepository;
import repository.WithPostgresContainer;

public class ProfileMergeTest extends WithPostgresContainer {

  private IdcsProfileAdapter idcsProfileAdapter;
  private SamlCiviFormProfileAdapter samlProfileAdapter;
  private ProfileFactory profileFactory;

  @Before
  public void setupFactory() {
    profileFactory = instanceOf(ProfileFactory.class);
    UserRepository repository = instanceOf(UserRepository.class);
    idcsProfileAdapter =
        new IdcsProfileAdapter(
            /* configuration = */ null,
            /* client = */ null,
            profileFactory,
            new Provider<UserRepository>() {
              @Override
              public UserRepository get() {
                return repository;
              }
            });
    samlProfileAdapter =
        new SamlCiviFormProfileAdapter(
            /* configuration = */ null,
            /* client = */ null,
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

    CiviFormProfileData profileData =
        idcsProfileAdapter.civiformProfileFromOidcProfile(oidcProfile);
    CiviFormProfile profile = profileFactory.wrapProfileData(profileData);

    assertThat(profileData.getEmail()).isEqualTo("foo@example.com");
    assertThat(profile.getEmailAddress().get()).isEqualTo("foo@example.com");
  }

  @Test
  public void testSuccessfulProfileMerge() {
    OidcProfile oidcProfile = new OidcProfile();
    oidcProfile.addAttribute("user_emailid", "foo@example.com");

    CiviFormProfileData profileData =
        idcsProfileAdapter.civiformProfileFromOidcProfile(oidcProfile);

    assertThat(
            idcsProfileAdapter
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

    CiviFormProfileData profileData =
        idcsProfileAdapter.civiformProfileFromOidcProfile(oidcProfile);

    assertThatThrownBy(
            () ->
                idcsProfileAdapter.mergeCiviFormProfile(
                    profileFactory.wrapProfileData(profileData), conflictingProfile))
        .hasCauseInstanceOf(ProfileMergeConflictException.class);
  }

  @Test
  public void testSamlProfileCreation() throws ExecutionException, InterruptedException {
    SAML2Profile saml2Profile = new SAML2Profile();
    saml2Profile.addAttribute(CommonProfileDefinition.EMAIL, "foo@example.com");

    CiviFormProfileData profileData =
        samlProfileAdapter.civiformProfileFromSamlProfile(saml2Profile);
    CiviFormProfile profile = profileFactory.wrapProfileData(profileData);

    assertThat(profileData.getEmail()).isEqualTo("foo@example.com");
    assertThat(profile.getEmailAddress().get()).isEqualTo("foo@example.com");
  }

  @Test
  public void testSuccessfulSamlProfileMerge() {
    SAML2Profile saml2Profile = new SAML2Profile();
    saml2Profile.addAttribute(CommonProfileDefinition.EMAIL, "foo@example.com");

    CiviFormProfileData profileData =
        samlProfileAdapter.civiformProfileFromSamlProfile(saml2Profile);

    assertThat(
            samlProfileAdapter
                .mergeCiviFormProfile(profileFactory.wrapProfileData(profileData), saml2Profile)
                .getEmail())
        .isEqualTo("foo@example.com");
  }

  @Test
  public void testFailedSamlProfileMerge() {
    SAML2Profile saml2Profile = new SAML2Profile();
    saml2Profile.addAttribute(CommonProfileDefinition.EMAIL, "foo@example.com");
    SAML2Profile conflictingProfile = new SAML2Profile();
    conflictingProfile.addAttribute(CommonProfileDefinition.EMAIL, "bar@example.com");

    CiviFormProfileData profileData =
        samlProfileAdapter.civiformProfileFromSamlProfile(saml2Profile);

    assertThatThrownBy(
            () ->
                samlProfileAdapter.mergeCiviFormProfile(
                    profileFactory.wrapProfileData(profileData), conflictingProfile))
        .hasCauseInstanceOf(ProfileMergeConflictException.class);
  }
}
