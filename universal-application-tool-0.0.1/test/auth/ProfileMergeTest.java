package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import auth.oidc.IdcsProfileAdapter;
import auth.oidc.InvalidOidcProfileException;
import auth.saml.SamlCiviFormProfileAdapter;
import io.ebean.DB;
import io.ebean.Database;
import java.util.concurrent.ExecutionException;
import javax.inject.Provider;
import models.Account;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.saml.profile.SAML2Profile;
import repository.ResetPostgres;
import repository.UserRepository;

public class ProfileMergeTest extends ResetPostgres {

  private IdcsProfileAdapter idcsProfileAdapter;
  private SamlCiviFormProfileAdapter samlProfileAdapter;
  private ProfileFactory profileFactory;
  private Database database;

  @Before
  public void setupDatabase() {
    database = DB.getDefault();
  }

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
    oidcProfile.addAttribute("iss", "issuer");
    oidcProfile.setId("subject");

    CiviFormProfileData profileData =
        idcsProfileAdapter.civiformProfileFromOidcProfile(oidcProfile);
    CiviFormProfile profile = profileFactory.wrapProfileData(profileData);

    assertThat(profileData.getEmail()).isEqualTo("foo@example.com");
    assertThat(profile.getEmailAddress().get()).isEqualTo("foo@example.com");
    assertThat(profile.getAuthorityId().get()).isEqualTo("iss: issuer sub: subject");
  }

  @Test
  public void testProfileMerge_oidc_succeeds_authorityPreviouslyMissing() throws Exception {

    // Setup an Account, but clear the authority_id to simulate the migration from IDing accounts by
    // email to authority_id.
    OidcProfile oidcProfile = new OidcProfile();
    oidcProfile.addAttribute("user_emailid", "foo@example.com");
    oidcProfile.addAttribute("iss", "issuer to delete");
    oidcProfile.setId("subject to delete");

    CiviFormProfileData existingProfileWithoutAuthority =
        idcsProfileAdapter.civiformProfileFromOidcProfile(oidcProfile);
    Account account =
        database.find(Account.class).where().eq("email_address", "foo@example.com").findOne();
    account.setAuthorityId(null);
    account.save();

    // Setup the expected OIDC state with authority_id data.
    OidcProfile oidcProfileWithAuthority = new OidcProfile();
    oidcProfileWithAuthority.addAttribute("user_emailid", "foo@example.com");
    oidcProfileWithAuthority.addAttribute("iss", "issuer");
    oidcProfileWithAuthority.setId("subject");

    CiviFormProfile mergedProfile =
        profileFactory.wrapProfileData(
            idcsProfileAdapter.mergeCiviFormProfile(
                profileFactory.wrapProfileData(existingProfileWithoutAuthority),
                oidcProfileWithAuthority));
    assertThat(mergedProfile.getEmailAddress().get()).isEqualTo("foo@example.com");
    assertThat(mergedProfile.getAuthorityId().get()).isEqualTo("iss: issuer sub: subject");
  }

  @Test
  public void testProfileMerge_oidc_succeeds() {
    OidcProfile oidcProfile = new OidcProfile();
    oidcProfile.addAttribute("user_emailid", "foo@example.com");
    oidcProfile.addAttribute("iss", "issuer");
    oidcProfile.setId("subject");

    CiviFormProfileData profileData =
        idcsProfileAdapter.civiformProfileFromOidcProfile(oidcProfile);

    assertThat(
            idcsProfileAdapter
                .mergeCiviFormProfile(profileFactory.wrapProfileData(profileData), oidcProfile)
                .getEmail())
        .isEqualTo("foo@example.com");
  }

  @Test
  public void testProfileMerge_fails_noUserEmailid() {
    OidcProfile oidcProfile = new OidcProfile();
    oidcProfile.addAttribute("user_emailid", "foo@example.com");
    oidcProfile.addAttribute("iss", "issuer");
    oidcProfile.setId("subject");

    OidcProfile newProfile = new OidcProfile();
    newProfile.addAttribute("iss", "issuer");
    newProfile.setId("subject");

    CiviFormProfileData profileData =
        idcsProfileAdapter.civiformProfileFromOidcProfile(oidcProfile);

    assertThatThrownBy(
            () ->
                idcsProfileAdapter.mergeCiviFormProfile(
                    profileFactory.wrapProfileData(profileData), newProfile))
        .isInstanceOf(InvalidOidcProfileException.class);
  }

  @Test
  public void testProfileMerge_fails_noSubject() {
    OidcProfile oidcProfile = new OidcProfile();
    oidcProfile.addAttribute("user_emailid", "foo@example.com");
    oidcProfile.addAttribute("iss", "issuer");
    oidcProfile.setId("subject");

    OidcProfile newProfile = new OidcProfile();
    newProfile.addAttribute("user_emailid", "foo@example.com");
    newProfile.addAttribute("iss", "issuer");

    CiviFormProfileData profileData =
        idcsProfileAdapter.civiformProfileFromOidcProfile(oidcProfile);

    assertThatThrownBy(
            () ->
                idcsProfileAdapter.mergeCiviFormProfile(
                    profileFactory.wrapProfileData(profileData), newProfile))
        .isInstanceOf(InvalidOidcProfileException.class);
  }

  @Test
  public void testProfileMerge_fails_noIssuer() {
    OidcProfile oidcProfile = new OidcProfile();
    oidcProfile.addAttribute("user_emailid", "foo@example.com");
    oidcProfile.addAttribute("iss", "issuer");
    oidcProfile.setId("subject");

    OidcProfile newProfile = new OidcProfile();
    newProfile.addAttribute("user_emailid", "foo@example.com");
    newProfile.setId("subject");

    CiviFormProfileData profileData =
        idcsProfileAdapter.civiformProfileFromOidcProfile(oidcProfile);

    assertThatThrownBy(
            () ->
                idcsProfileAdapter.mergeCiviFormProfile(
                    profileFactory.wrapProfileData(profileData), newProfile))
        .isInstanceOf(InvalidOidcProfileException.class);
  }

  @Test
  public void testProfileMerge_fails_emailsDoNotMatch() {
    OidcProfile oidcProfile = new OidcProfile();
    oidcProfile.addAttribute("user_emailid", "foo@example.com");
    oidcProfile.addAttribute("iss", "issuer");
    oidcProfile.setId("subject");

    OidcProfile conflictingProfile = new OidcProfile();
    conflictingProfile.addAttribute("user_emailid", "bar@example.com");
    conflictingProfile.addAttribute("iss", "issuer");
    conflictingProfile.setId("subject");

    CiviFormProfileData profileData =
        idcsProfileAdapter.civiformProfileFromOidcProfile(oidcProfile);

    assertThatThrownBy(
            () ->
                idcsProfileAdapter.mergeCiviFormProfile(
                    profileFactory.wrapProfileData(profileData), conflictingProfile))
        .hasCauseInstanceOf(ProfileMergeConflictException.class);
  }

  @Test
  public void testProfileMerge_oidc_fails_differentAuthoritySubject() {
    OidcProfile oidcProfile = new OidcProfile();
    oidcProfile.addAttribute("user_emailid", "foo@example.com");
    oidcProfile.addAttribute("iss", "issuer");
    oidcProfile.setId("subject");

    OidcProfile conflictingProfile = new OidcProfile();
    conflictingProfile.addAttribute("user_emailid", "bar@example.com");
    conflictingProfile.addAttribute("iss", "issuer");
    conflictingProfile.setId("a different subject");

    CiviFormProfileData profileData =
        idcsProfileAdapter.civiformProfileFromOidcProfile(oidcProfile);

    assertThatThrownBy(
            () ->
                idcsProfileAdapter.mergeCiviFormProfile(
                    profileFactory.wrapProfileData(profileData), conflictingProfile))
        .hasCauseInstanceOf(ProfileMergeConflictException.class);
  }

  @Test
  public void testProfileMerge_oidc_fails_differentAuthorityIssuer() {
    OidcProfile oidcProfile = new OidcProfile();
    oidcProfile.addAttribute("user_emailid", "foo@example.com");
    oidcProfile.addAttribute("iss", "issuer");
    oidcProfile.setId("subject");

    OidcProfile conflictingProfile = new OidcProfile();
    conflictingProfile.addAttribute("user_emailid", "bar@example.com");
    conflictingProfile.addAttribute("iss", "a different issuer");
    conflictingProfile.setId("subject");

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
