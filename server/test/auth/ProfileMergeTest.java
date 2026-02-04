package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static support.FakeRequestBuilder.fakeRequest;

import auth.oidc.InvalidOidcProfileException;
import auth.oidc.OidcClientProviderParams;
import auth.oidc.StandardClaimsAttributeNames;
import auth.oidc.applicant.IdcsApplicantProfileCreator;
import auth.saml.InvalidSamlProfileException;
import auth.saml.SamlProfileCreator;
import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import models.AccountModel;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.play.PlayWebContext;
import org.pac4j.saml.profile.SAML2Profile;
import repository.AccountRepository;
import repository.ResetPostgres;
import support.CfTestHelpers;

public class ProfileMergeTest extends ResetPostgres {

  private IdcsApplicantProfileCreator idcsApplicantProfileCreator;
  private SamlProfileCreator samlProfileCreator;
  private ProfileFactory profileFactory;
  private static AccountRepository accountRepository;
  private Database database;
  private WebContext context;

  @Before
  public void setupDatabase() {
    database = DB.getDefault();
  }

  @Before
  public void setupFactory() {
    profileFactory = instanceOf(ProfileFactory.class);
    accountRepository = instanceOf(AccountRepository.class);
    OidcClient client = CfTestHelpers.getOidcClient("dev-oidc", 3390);
    OidcConfiguration client_config = CfTestHelpers.getOidcConfiguration("dev-oidc", 3390);

    StandardClaimsAttributeNames standardClaimsAttributeNames =
        StandardClaimsAttributeNames.builder()
            .setEmail("user_emailid")
            .setLocale(Optional.of("user_locale"))
            .setNames(ImmutableList.of("user_displayname"))
            .setPhoneNumber(Optional.of("phone_number"))
            .build();

    // Just need some complete adaptor to access methods.
    idcsApplicantProfileCreator =
        new IdcsApplicantProfileCreator(
            client_config,
            client,
            OidcClientProviderParams.create(
                profileFactory, CfTestHelpers.userRepositoryProvider(accountRepository)),
            standardClaimsAttributeNames);
    samlProfileCreator =
        new SamlProfileCreator(
            /* configuration= */ null,
            /* client= */ null,
            profileFactory,
            CfTestHelpers.userRepositoryProvider(accountRepository));
    context = new PlayWebContext(fakeRequest());
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
        idcsApplicantProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), oidcProfile, context);
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
        idcsApplicantProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), oidcProfile, context);
    AccountModel account =
        database.find(AccountModel.class).where().eq("email_address", "foo@example.com").findOne();
    account.setAuthorityId(null);
    account.save();

    // Setup the expected OIDC state with authority_id data.
    OidcProfile oidcProfileWithAuthority =
        createOidcProfile("foo@example.com", "issuer", "subject");

    CiviFormProfile mergedProfile =
        profileFactory.wrapProfileData(
            idcsApplicantProfileCreator.mergeCiviFormProfile(
                Optional.of(profileFactory.wrapProfileData(existingProfileWithoutAuthority)),
                oidcProfileWithAuthority,
                context));
    assertThat(mergedProfile.getEmailAddress().get()).isEqualTo("foo@example.com");
    assertThat(mergedProfile.getAuthorityId().get()).isEqualTo("iss: issuer sub: subject");
  }

  @Test
  public void testProfileMerge_oidc_succeeds() {
    OidcProfile oidcProfile = createOidcProfile("foo@example.com", "issuer", "subject");

    CiviFormProfileData profileData =
        idcsApplicantProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), oidcProfile, context);

    assertThat(
            idcsApplicantProfileCreator
                .mergeCiviFormProfile(
                    Optional.of(profileFactory.wrapProfileData(profileData)), oidcProfile, context)
                .getEmail())
        .isEqualTo("foo@example.com");

    assertThat(idcsApplicantProfileCreator.getExistingApplicant(oidcProfile).get().getPhoneNumber())
        .isEqualTo(Optional.empty());
  }

  @Test
  public void testProfileMerge_oidc_with_phone_succeeds() {
    OidcProfile oidcProfile = createOidcProfile("foo@example.com", "issuer", "subject");
    oidcProfile.addAttribute("phone_number", "253-555-1122");

    CiviFormProfileData profileData =
        idcsApplicantProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), oidcProfile, context);

    idcsApplicantProfileCreator.mergeCiviFormProfile(
        Optional.of(profileFactory.wrapProfileData(profileData)), oidcProfile, context);

    assertThat(idcsApplicantProfileCreator.getExistingApplicant(oidcProfile).get().getPhoneNumber())
        .isEqualTo(Optional.of("2535551122"));
  }

  @Test
  public void testProfileMerge_fails_noUserEmailid() {
    OidcProfile oidcProfile = createOidcProfile("foo@example.com", "issuer", "subject");

    OidcProfile newProfile = new OidcProfile();
    newProfile.addAttribute("iss", "issuer");
    newProfile.setId("subject");

    CiviFormProfileData profileData =
        idcsApplicantProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), oidcProfile, context);

    assertThatThrownBy(
            () ->
                idcsApplicantProfileCreator.mergeCiviFormProfile(
                    Optional.of(profileFactory.wrapProfileData(profileData)), newProfile, context))
        .isInstanceOf(InvalidOidcProfileException.class);
  }

  @Test
  public void testProfileMerge_fails_noSubject() {
    OidcProfile oidcProfile = createOidcProfile("foo@example.com", "issuer", "subject");

    OidcProfile newProfile = new OidcProfile();
    newProfile.addAttribute("user_emailid", "foo@example.com");
    newProfile.addAttribute("iss", "issuer");

    CiviFormProfileData profileData =
        idcsApplicantProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), oidcProfile, context);

    assertThatThrownBy(
            () ->
                idcsApplicantProfileCreator.mergeCiviFormProfile(
                    Optional.of(profileFactory.wrapProfileData(profileData)), newProfile, context))
        .isInstanceOf(InvalidOidcProfileException.class);
  }

  @Test
  public void testProfileMerge_fails_noIssuer() {
    OidcProfile oidcProfile = createOidcProfile("foo@example.com", "issuer", "subject");

    OidcProfile newProfile = new OidcProfile();
    newProfile.addAttribute("user_emailid", "foo@example.com");
    newProfile.setId("subject");

    CiviFormProfileData profileData =
        idcsApplicantProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), oidcProfile, context);

    assertThatThrownBy(
            () ->
                idcsApplicantProfileCreator.mergeCiviFormProfile(
                    Optional.of(profileFactory.wrapProfileData(profileData)), newProfile, context))
        .isInstanceOf(InvalidOidcProfileException.class);
  }

  @Test
  public void testProfileMerge_fails_emailsDoNotMatch() {
    OidcProfile oidcProfile = createOidcProfile("foo@example.com", "issuer", "subject");
    OidcProfile conflictingProfile = createOidcProfile("bar@example.com", "issuer", "subject");

    CiviFormProfileData profileData =
        idcsApplicantProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), oidcProfile, context);

    assertThatThrownBy(
            () ->
                idcsApplicantProfileCreator.mergeCiviFormProfile(
                    Optional.of(profileFactory.wrapProfileData(profileData)),
                    conflictingProfile,
                    context))
        .hasCauseInstanceOf(ProfileMergeConflictException.class);
  }

  @Test
  public void testProfileMerge_oidc_fails_differentAuthoritySubject() {
    OidcProfile oidcProfile = createOidcProfile("foo@example.com", "issuer", "subject");
    OidcProfile conflictingProfile =
        createOidcProfile("foo@example.com", "issuer", "a different subject");

    CiviFormProfileData profileData =
        idcsApplicantProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), oidcProfile, context);

    assertThatThrownBy(
            () ->
                idcsApplicantProfileCreator.mergeCiviFormProfile(
                    Optional.of(profileFactory.wrapProfileData(profileData)),
                    conflictingProfile,
                    context))
        .hasCauseInstanceOf(ProfileMergeConflictException.class);
  }

  @Test
  public void testProfileMerge_oidc_fails_differentAuthorityIssuer() {
    OidcProfile oidcProfile = createOidcProfile("foo@example.com", "issuer", "subject");
    OidcProfile conflictingProfile =
        createOidcProfile("foo@example.com", "a different issuer", "subject");

    CiviFormProfileData profileData =
        idcsApplicantProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), oidcProfile, context);

    assertThatThrownBy(
            () ->
                idcsApplicantProfileCreator.mergeCiviFormProfile(
                    Optional.of(profileFactory.wrapProfileData(profileData)),
                    conflictingProfile,
                    context))
        .hasCauseInstanceOf(ProfileMergeConflictException.class);
  }

  @Test
  public void testSamlProfileCreation() throws Exception {
    SAML2Profile saml2Profile = createSamlProfile("foo@example.com", "issuer", "subject");

    CiviFormProfileData profileData =
        samlProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), saml2Profile);
    CiviFormProfile profile = profileFactory.wrapProfileData(profileData);

    assertThat(profileData.getEmail()).isEqualTo("foo@example.com");
    assertThat(profile.getEmailAddress().get()).isEqualTo("foo@example.com");
    assertThat(profile.getAuthorityId().get()).isEqualTo("Issuer: issuer NameID: subject");
  }

  @Test
  public void testProfileMerge_saml_succeeds() {
    SAML2Profile saml2Profile = createSamlProfile("foo@example.com", "issuer", "subject");

    CiviFormProfileData profileData =
        samlProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), saml2Profile);

    assertThat(
            samlProfileCreator
                .mergeCiviFormProfile(
                    Optional.of(profileFactory.wrapProfileData(profileData)), saml2Profile)
                .getEmail())
        .isEqualTo("foo@example.com");
  }

  @Test
  public void testProfileMerge_saml_fails_differentEmails() {
    SAML2Profile saml2Profile = createSamlProfile("foo@example.com", "issuer", "subject");
    SAML2Profile conflictingProfile = createSamlProfile("bar@example.com", "issuer", "subject");

    CiviFormProfileData profileData =
        samlProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), saml2Profile);

    assertThatThrownBy(
            () ->
                samlProfileCreator.mergeCiviFormProfile(
                    Optional.of(profileFactory.wrapProfileData(profileData)), conflictingProfile))
        .hasCauseInstanceOf(ProfileMergeConflictException.class);
  }

  @Test
  public void testProfileMerge_saml_fails_noSubject() {
    SAML2Profile saml2Profile = createSamlProfile("foo@example.com", "issuer", "subject");

    SAML2Profile newProfile = new SAML2Profile();
    newProfile.addAttribute(CommonProfileDefinition.EMAIL, "foo@example.com");
    newProfile.addAuthenticationAttribute("issuerId", "issuer");

    CiviFormProfileData profileData =
        samlProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), saml2Profile);

    assertThatThrownBy(
            () ->
                samlProfileCreator.mergeCiviFormProfile(
                    Optional.of(profileFactory.wrapProfileData(profileData)), newProfile))
        .isInstanceOf(InvalidSamlProfileException.class);
  }

  @Test
  public void testProfileMerge_saml_fails_noIssuer() {
    SAML2Profile saml2Profile = createSamlProfile("foo@example.com", "issuer", "subject");

    SAML2Profile newProfile = new SAML2Profile();
    newProfile.addAttribute(CommonProfileDefinition.EMAIL, "foo@example.com");
    newProfile.setId("subject");

    CiviFormProfileData profileData =
        samlProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), saml2Profile);

    assertThatThrownBy(
            () ->
                samlProfileCreator.mergeCiviFormProfile(
                    Optional.of(profileFactory.wrapProfileData(profileData)), newProfile))
        .isInstanceOf(InvalidSamlProfileException.class);
  }

  @Test
  public void testProfileMerge_saml_fails_differentAuthoritySubject() {
    SAML2Profile saml2Profile = createSamlProfile("foo@example.com", "issuer", "subject");
    SAML2Profile conflictingProfile =
        createSamlProfile("foo@example.com", "issuer", "a different subject");

    CiviFormProfileData profileData =
        samlProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), saml2Profile);

    assertThatThrownBy(
            () ->
                samlProfileCreator.mergeCiviFormProfile(
                    Optional.of(profileFactory.wrapProfileData(profileData)), conflictingProfile))
        .isInstanceOf(CompletionException.class);
  }

  @Test
  public void testProfileMerge_saml_fails_differentAuthorityIssuer() {
    SAML2Profile saml2Profile = createSamlProfile("foo@example.com", "issuer", "subject");
    SAML2Profile conflictingProfile =
        createSamlProfile("foo@example.com", "a different issuer", "subject");

    CiviFormProfileData profileData =
        samlProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), saml2Profile);

    assertThatThrownBy(
            () ->
                samlProfileCreator.mergeCiviFormProfile(
                    Optional.of(profileFactory.wrapProfileData(profileData)), conflictingProfile))
        .isInstanceOf(CompletionException.class);
  }
}
