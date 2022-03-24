package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import auth.oidc.IdcsProfileAdapter;
import auth.oidc.InvalidOidcProfileException;
import auth.saml.InvalidSamlProfileException;
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

  private OidcProfile createOidcProfile(String email, String issuer, String subject) {
    OidcProfile profile = new OidcProfile();
    profile.addAttribute("user_emailid", email);
    profile.addAttribute("iss", issuer);
    profile.setId(subject);
    return profile;
  }

  private SAML2Profile createSamlProfile(String email, String issuer, String subject) {
    SAML2Profile profile = new SAML2Profile();
    profile.setId(subject);
    profile.addAuthenticationAttribute("issuerId", issuer);
    profile.addAttribute(CommonProfileDefinition.EMAIL, email);
    return profile;
  }

  @Test
  public void testProfileCreation() throws ExecutionException, InterruptedException {
    OidcProfile oidcProfile = createOidcProfile("foo@example.com", "issuer", "subject");

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
    OidcProfile oidcProfile =
        createOidcProfile("foo@example.com", "issuer to delete", "subject to delete");

    CiviFormProfileData existingProfileWithoutAuthority =
        idcsProfileAdapter.civiformProfileFromOidcProfile(oidcProfile);
    Account account =
        database.find(Account.class).where().eq("email_address", "foo@example.com").findOne();
    account.setAuthorityId(null);
    account.save();

    // Setup the expected OIDC state with authority_id data.
    OidcProfile oidcProfileWithAuthority =
        createOidcProfile("foo@example.com", "issuer", "subject");

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
    OidcProfile oidcProfile = createOidcProfile("foo@example.com", "issuer", "subject");

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
    OidcProfile oidcProfile = createOidcProfile("foo@example.com", "issuer", "subject");

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
    OidcProfile oidcProfile = createOidcProfile("foo@example.com", "issuer", "subject");

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
    OidcProfile oidcProfile = createOidcProfile("foo@example.com", "issuer", "subject");

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
    OidcProfile oidcProfile = createOidcProfile("foo@example.com", "issuer", "subject");
    OidcProfile conflictingProfile = createOidcProfile("bar@example.com", "issuer", "subject");

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
    OidcProfile oidcProfile = createOidcProfile("foo@example.com", "issuer", "subject");
    OidcProfile conflictingProfile =
        createOidcProfile("foo@example.com", "issuer", "a different subject");

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
    OidcProfile oidcProfile = createOidcProfile("foo@example.com", "issuer", "subject");
    OidcProfile conflictingProfile =
        createOidcProfile("foo@example.com", "a different issuer", "subject");

    CiviFormProfileData profileData =
        idcsProfileAdapter.civiformProfileFromOidcProfile(oidcProfile);

    assertThatThrownBy(
            () ->
                idcsProfileAdapter.mergeCiviFormProfile(
                    profileFactory.wrapProfileData(profileData), conflictingProfile))
        .hasCauseInstanceOf(ProfileMergeConflictException.class);
  }

  @Test
  public void testSamlProfileCreation() throws Exception {
    SAML2Profile saml2Profile = createSamlProfile("foo@example.com", "issuer", "subject");

    CiviFormProfileData profileData =
        samlProfileAdapter.civiformProfileFromSamlProfile(saml2Profile);
    CiviFormProfile profile = profileFactory.wrapProfileData(profileData);

    assertThat(profileData.getEmail()).isEqualTo("foo@example.com");
    assertThat(profile.getEmailAddress().get()).isEqualTo("foo@example.com");
    assertThat(profile.getAuthorityId().get()).isEqualTo("iss: issuer sub: subject");
  }

  @Test
  public void testProfileMerge_saml_succeeds() {
    SAML2Profile saml2Profile = createSamlProfile("foo@example.com", "issuer", "subject");

    CiviFormProfileData profileData =
        samlProfileAdapter.civiformProfileFromSamlProfile(saml2Profile);

    assertThat(
            samlProfileAdapter
                .mergeCiviFormProfile(profileFactory.wrapProfileData(profileData), saml2Profile)
                .getEmail())
        .isEqualTo("foo@example.com");
  }

  @Test
  public void testProfileMerge_saml_fails_differentEmails() {
    SAML2Profile saml2Profile = createSamlProfile("foo@example.com", "issuer", "subject");
    SAML2Profile conflictingProfile = createSamlProfile("bar@example.com", "issuer", "subject");

    CiviFormProfileData profileData =
        samlProfileAdapter.civiformProfileFromSamlProfile(saml2Profile);

    assertThatThrownBy(
            () ->
                samlProfileAdapter.mergeCiviFormProfile(
                    profileFactory.wrapProfileData(profileData), conflictingProfile))
        .hasCauseInstanceOf(ProfileMergeConflictException.class);
  }

  @Test
  public void testProfileMerge_saml_fails_noSubject() {
    SAML2Profile saml2Profile = createSamlProfile("foo@example.com", "issuer", "subject");

    SAML2Profile newProfile = new SAML2Profile();
    newProfile.addAttribute(CommonProfileDefinition.EMAIL, "foo@example.com");
    newProfile.addAuthenticationAttribute("issuerId", "issuer");

    CiviFormProfileData profileData =
        samlProfileAdapter.civiformProfileFromSamlProfile(saml2Profile);

    assertThatThrownBy(
            () ->
                samlProfileAdapter.mergeCiviFormProfile(
                    profileFactory.wrapProfileData(profileData), newProfile))
        .isInstanceOf(InvalidSamlProfileException.class);
  }

  @Test
  public void testProfileMerge_saml_fails_noIssuer() {
    SAML2Profile saml2Profile = createSamlProfile("foo@example.com", "issuer", "subject");

    SAML2Profile newProfile = new SAML2Profile();
    newProfile.addAttribute(CommonProfileDefinition.EMAIL, "foo@example.com");
    newProfile.setId("subject");

    CiviFormProfileData profileData =
        samlProfileAdapter.civiformProfileFromSamlProfile(saml2Profile);

    assertThatThrownBy(
            () ->
                samlProfileAdapter.mergeCiviFormProfile(
                    profileFactory.wrapProfileData(profileData), newProfile))
        .isInstanceOf(InvalidSamlProfileException.class);
  }

  @Test
  public void testProfileMerge_saml_fails_differentAuthoritySubject() {
    SAML2Profile saml2Profile = createSamlProfile("foo@example.com", "issuer", "subject");
    SAML2Profile conflictingProfile =
        createSamlProfile("foo@example.com", "issuer", "a different subject");

    CiviFormProfileData profileData =
        samlProfileAdapter.civiformProfileFromSamlProfile(saml2Profile);

    assertThatThrownBy(
            () ->
                samlProfileAdapter.mergeCiviFormProfile(
                    profileFactory.wrapProfileData(profileData), conflictingProfile))
        .isInstanceOf(ProfileMergeConflictException.class);
  }

  @Test
  public void testProfileMerge_saml_fails_differentAuthorityIssuer() {
    SAML2Profile saml2Profile = createSamlProfile("foo@example.com", "issuer", "subject");
    SAML2Profile conflictingProfile =
        createSamlProfile("foo@example.com", "a different issuer", "subject");

    CiviFormProfileData profileData =
        samlProfileAdapter.civiformProfileFromSamlProfile(saml2Profile);

    assertThatThrownBy(
            () ->
                samlProfileAdapter.mergeCiviFormProfile(
                    profileFactory.wrapProfileData(profileData), conflictingProfile))
        .isInstanceOf(ProfileMergeConflictException.class);
  }
}
