package auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.oidc.applicant.IdcsApplicantProfileCreator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.AccountModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pac4j.core.profile.BasicUserProfile;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import repository.AccountRepository;
import repository.DatabaseExecutionContext;
import repository.ResetPostgres;
import support.CfTestHelpers;

@RunWith(JUnitParamsRunner.class)
public class CiviformOidcProfileCreatorTest extends ResetPostgres {
  private static final String EMAIL = "foo@bar.com";
  private static final String NAME = "Philip J. Fry";
  private static final String ISSUER = "issuer";
  private static final String SUBJECT = "subject";
  private static final String AUTHORITY_ID = "iss: issuer sub: subject";
  private static final String ID_TOKEN_STRING = "id token string";
  private static final String PHONE_NUMBER = "2535554321";
  private static final String PHONE_NUMBER_ATTRIBUTE_NAME = "phone_number";

  private OidcProfile oidcProfile;
  private ProfileFactory profileFactory;
  private AccountRepository accountRepository;
  private TrustedIntermediaryGroupModel tiGroup;

  @Before
  public void setup() {
    accountRepository = instanceOf(AccountRepository.class);
    profileFactory = instanceOf(ProfileFactory.class);

    oidcProfile = new OidcProfile();
    oidcProfile.addAttribute("user_emailid", EMAIL);
    oidcProfile.addAttribute("user_displayname", NAME);
    oidcProfile.addAttribute("user_locale", "fr");
    oidcProfile.addAttribute("iss", ISSUER);
    oidcProfile.setId(SUBJECT);
    oidcProfile.setIdTokenString(ID_TOKEN_STRING);

    tiGroup = new TrustedIntermediaryGroupModel("TI Group", "test");
    tiGroup.save();
  }

  private CiviformOidcProfileCreator getOidcProfileCreator(
      boolean enhancedLogoutEnabled, boolean includePhoneScope) {
    ImmutableMap.Builder<String, Object> configMapBuilder =
        ImmutableMap.<String, Object>builder()
            .put("applicant_oidc_enhanced_logout_enabled", String.valueOf(enhancedLogoutEnabled));

    Optional<String> phoneNumberAttribute = Optional.empty();

    if (includePhoneScope) {
      configMapBuilder
          .put("applicant_generic_oidc.additional_scopes", "phone")
          .put("idcs.phone_number_attribute", PHONE_NUMBER_ATTRIBUTE_NAME);

      phoneNumberAttribute = Optional.of(PHONE_NUMBER_ATTRIBUTE_NAME);
    }

    Config civiformConfig = ConfigFactory.parseMap(configMapBuilder.build());
    OidcClient client = CfTestHelpers.getOidcClient("dev-oidc", 3390);
    OidcConfiguration oidcConfig = CfTestHelpers.getOidcConfiguration("dev-oidc", 3390);

    StandardClaimsAttributeNames standardClaimsAttributeNames =
        StandardClaimsAttributeNames.builder()
            .setEmail("user_emailid")
            .setLocale(Optional.of("user_locale"))
            .setNames(ImmutableList.of("user_displayname"))
            .setPhoneNumber(phoneNumberAttribute)
            .build();

    return new IdcsApplicantProfileCreator(
        oidcConfig,
        client,
        OidcClientProviderParams.create(
            civiformConfig,
            profileFactory,
            CfTestHelpers.userRepositoryProvider(accountRepository)),
        standardClaimsAttributeNames,
        instanceOf(DatabaseExecutionContext.class),
        null);
  }

  private CiviformOidcProfileCreator getOidcProfileCreator() {
    return getOidcProfileCreator(false, false);
  }

  private CiviformOidcProfileCreator getOidcProfileCreatorWithEnhancedLogoutEnabled() {
    return getOidcProfileCreator(true, false);
  }

  private CiviformOidcProfileCreator getOidcProfileCreatorWithPhoneScope() {
    return getOidcProfileCreator(false, true);
  }

  @Test
  public void getExistingApplicant_succeeds_noAuthorityFallsBackToEmail() {
    // When an existing account doesn't have an authority_id we still find it by
    // email.

    // Setup.
    // Existing account doesn't have an authority.
    var emailAccount =
        resourceCreator
            .insertAccount()
            .setEmailAddress(EMAIL)
            .setApplicants(ImmutableList.of(resourceCreator.insertApplicant()));
    emailAccount.save();
    CiviformOidcProfileCreator oidcProfileCreator = getOidcProfileCreator();

    // Execute.
    Optional<ApplicantModel> applicant = oidcProfileCreator.getExistingApplicant(oidcProfile);

    // Verify.
    assertThat(applicant).isPresent();
    AccountModel account = applicant.get().getAccount();

    assertThat(account.id).isEqualTo(emailAccount.id);
    assertThat(account.getEmailAddress()).isEqualTo(EMAIL);
    // The existing account doesn't have an authority as it didn't before.
    assertThat(account.getAuthorityId()).isNull();
  }

  @Test
  public void getExistingApplicant_succeeds_sameAuthorityDifferentEmail() {
    // Authority ID is the main key and returns the local account even with
    // different other old keys like email.

    // Setup.
    final String otherEmail = "OTHER@EMAIL.com";
    // Existing account has authority but some other email.
    var existingAccount =
        resourceCreator
            .insertAccount()
            .setEmailAddress(otherEmail)
            .setAuthorityId(AUTHORITY_ID)
            .setApplicants(ImmutableList.of(resourceCreator.insertApplicant()));
    existingAccount.save();
    CiviformOidcProfileCreator oidcProfileCreator = getOidcProfileCreator();

    // Execute.
    Optional<ApplicantModel> applicant = oidcProfileCreator.getExistingApplicant(oidcProfile);

    // Verify.
    assertThat(applicant).isPresent();
    AccountModel account = applicant.get().getAccount();

    assertThat(account.id).isEqualTo(existingAccount.id);
    // The email of the existing account is the pre-existing one, not a new profile
    // one.
    assertThat(account.getEmailAddress()).isEqualTo(otherEmail);
    assertThat(account.getAuthorityId()).isEqualTo(AUTHORITY_ID);
  }

  @Test
  public void mergeCiviFormProfile_succeeds_new_user() {
    CiviformOidcProfileCreator oidcProfileCreator = getOidcProfileCreator();
    // Execute.
    CiviFormProfileData profileData =
        oidcProfileCreator.mergeCiviFormProfile(
            /* maybeCiviFormProfile= */ Optional.empty(), oidcProfile);

    // Verify.
    assertThat(profileData).isNotNull();

    assertThat(profileData.getEmail()).isEqualTo(EMAIL);

    Optional<ApplicantModel> maybeApplicant = oidcProfileCreator.getExistingApplicant(oidcProfile);
    assertThat(maybeApplicant).isPresent();

    // The session ID is a random value, so just ensure it's set and not an empty string.
    assertThat(profileData.getSessionId()).isNotEmpty();

    ApplicantModel applicant = maybeApplicant.get();

    assertThat(applicant.getApplicantName().orElse("<empty optional>")).isEqualTo("Fry, Philip");
    Locale l = applicant.getApplicantData().preferredLocale();
    assertThat(l).isEqualTo(Locale.FRENCH);
  }

  @Test
  public void mergeCiviFormProfile_existingUser_maintainsExistingData() {
    // This tests that when merging an OIDC profile into an existing CiviForm profile
    // (created via profileFactory), the session ID and account ID are preserved.
    CiviformOidcProfileCreator profileCreator = getOidcProfileCreator();
    OidcProfile oidcProfile = makeOidcProfile();
    CiviFormProfileData existingProfileData = profileFactory.createNewApplicant();
    CiviFormProfile existingProfile = profileFactory.wrapProfileData(existingProfileData);

    CiviFormProfileData mergedProfileData =
        profileCreator.mergeCiviFormProfile(Optional.of(existingProfile), oidcProfile);

    // The session ID and account ID should be unchanged after merging.
    assertThat(existingProfileData.getSessionId()).isEqualTo(mergedProfileData.getSessionId());
    assertThat(existingProfileData.getId()).isEqualTo(mergedProfileData.getId());
  }

  @Test
  public void mergeCiviFormProfile_succeeds_new_user_with_enhanced_logout() {
    CiviformOidcProfileCreator oidcProfileCreator =
        getOidcProfileCreatorWithEnhancedLogoutEnabled();

    // Execute.
    CiviFormProfileData profileData =
        oidcProfileCreator.mergeCiviFormProfile(Optional.empty(), oidcProfile);

    // Verify.
    assertThat(profileData).isNotNull();

    assertThat(profileData.getEmail()).isEqualTo(EMAIL);

    Optional<ApplicantModel> maybeApplicant = oidcProfileCreator.getExistingApplicant(oidcProfile);
    assertThat(maybeApplicant).isPresent();

    ApplicantModel applicant = maybeApplicant.get();

    assertThat(applicant.getApplicantName().orElse("<empty optional>")).isEqualTo("Fry, Philip");
    Locale l = applicant.getApplicantData().preferredLocale();
    assertThat(l).isEqualTo(Locale.FRENCH);

    // Additional validations for enhanced logout behavior.
    AccountModel account = maybeApplicant.get().getAccount();
    IdTokens idTokens = account.getIdTokens();
    assertThat(idTokens.getIdToken(profileData.getSessionId())).hasValue(ID_TOKEN_STRING);
  }

  @Test
  public void mergeCiviFormProfile_skipped_forTrustedIntermediaries() {
    ApplicantModel tiApplicant = makeTrustedIntermediary();

    CiviFormProfile trustedIntermediary = profileFactory.wrap(tiApplicant);
    String originalProfileId = trustedIntermediary.getProfileData().getId();

    CiviformOidcProfileCreator oidcProfileCreator = getOidcProfileCreator();

    // Execute.
    CiviFormProfileData profileData =
        oidcProfileCreator.mergeCiviFormProfile(Optional.of(trustedIntermediary), oidcProfile);

    // Verify.
    // Profile data should still be present after the no-op merge.
    assertThat(profileData).isNotNull();
    // The profile ID should be unchanged (no merge happened).
    assertThat(profileData.getId()).isEqualTo(originalProfileId);

    // email is set
    assertThat(profileData.getEmail()).isEqualTo(EMAIL);
    assertThat(profileData.getDisplayName()).isNull();

    // The OIDC profile should not have created a new applicant.
    Optional<ApplicantModel> maybeApplicant = oidcProfileCreator.getExistingApplicant(oidcProfile);
    assertThat(maybeApplicant).isNotPresent();
  }

  @Test
  public void mergeCiviFormProfile_skippedWithEnhancedLogout_forTrustedIntermediaries() {
    ApplicantModel tiApplicant = makeTrustedIntermediary();
    AccountModel tiAccount = tiApplicant.getAccount();

    CiviFormProfile trustedIntermediary = profileFactory.wrap(tiApplicant);
    String originalProfileId = trustedIntermediary.getProfileData().getId();

    CiviformOidcProfileCreator oidcProfileCreator =
        getOidcProfileCreatorWithEnhancedLogoutEnabled();

    // Execute.
    CiviFormProfileData profileData =
        oidcProfileCreator.mergeCiviFormProfile(Optional.of(trustedIntermediary), oidcProfile);

    // Verify.
    // Profile data should still be present after the no-op merge.
    assertThat(profileData).isNotNull();
    // The profile ID should be unchanged (no merge happened).
    assertThat(profileData.getId()).isEqualTo(originalProfileId);

    // email is set
    assertThat(profileData.getEmail()).isEqualTo(EMAIL);
    assertThat(profileData.getDisplayName()).isNull();

    // The OIDC profile should not have created a new applicant.
    Optional<ApplicantModel> maybeApplicant = oidcProfileCreator.getExistingApplicant(oidcProfile);
    assertThat(maybeApplicant).isNotPresent();

    // Additional validations for enhanced logout behavior.
    // Refresh the account to get the latest id_tokens
    tiAccount.refresh();
    IdTokens idTokens = tiAccount.getIdTokens();
    assertThat(idTokens.getIdToken(profileData.getSessionId())).hasValue(ID_TOKEN_STRING);
  }

  private Object[] allowedPhoneNumbers() {
    return new Object[] {
      // Various US phone number formats
      new Object[] {"+12535551122", "2535551122"},
      new Object[] {"253-555-1122", "2535551122"},
      new Object[] {"(253) 555-1122", "2535551122"},
    };
  }

  @Test
  @Parameters(method = "allowedPhoneNumbers")
  public void mergeCiviFormProfile_withPhoneScope_importAllowedPhoneNumber(
      String phoneNumber, String cleanedPhoneNumber) {
    CiviformOidcProfileCreator oidcProfileCreator = getOidcProfileCreatorWithPhoneScope();
    // Execute.

    oidcProfile.addAttribute(PHONE_NUMBER_ATTRIBUTE_NAME, phoneNumber);

    CiviFormProfileData profileData =
        oidcProfileCreator.mergeCiviFormProfile(Optional.empty(), oidcProfile);

    // Verify.
    assertThat(profileData).isNotNull();

    Optional<ApplicantModel> maybeApplicant = oidcProfileCreator.getExistingApplicant(oidcProfile);
    assertThat(maybeApplicant).isPresent();
    assertThat(maybeApplicant.get().getPhoneNumber()).isEqualTo(Optional.of(cleanedPhoneNumber));
  }

  private Object[] disallowedPhoneNumbers() {
    return new Object[] {
      // E164 format phone number for GB
      new Object[] {"+447911123456"},
      new Object[] {"asdfklajsdfasdf"},
      new Object[] {""},
      new Object[] {null},
    };
  }

  @Test
  @Parameters(method = "disallowedPhoneNumbers")
  public void mergeCiviFormProfile_withPhoneScope_doesNotImportInvalidPhoneNumber(
      String phoneNumber) {
    CiviformOidcProfileCreator oidcProfileCreator = getOidcProfileCreatorWithPhoneScope();
    // Execute.
    oidcProfile.addAttribute(PHONE_NUMBER_ATTRIBUTE_NAME, phoneNumber);

    CiviFormProfileData profileData =
        oidcProfileCreator.mergeCiviFormProfile(Optional.empty(), oidcProfile);

    // Verify.
    assertThat(profileData).isNotNull();

    Optional<ApplicantModel> maybeApplicant = oidcProfileCreator.getExistingApplicant(oidcProfile);
    assertThat(maybeApplicant).isPresent();
    assertThat(maybeApplicant.get().getPhoneNumber()).isEqualTo(Optional.empty());
  }

  @Test
  public void mergeCiviFormProfile_withoutPhoneScope_doesNotImportPhoneNumber() {
    // This does NOT have the phone scope requested
    CiviformOidcProfileCreator oidcProfileCreator = getOidcProfileCreator();

    // Execute.
    oidcProfile.addAttribute(PHONE_NUMBER_ATTRIBUTE_NAME, PHONE_NUMBER);

    CiviFormProfileData profileData =
        oidcProfileCreator.mergeCiviFormProfile(Optional.empty(), oidcProfile);

    // Verify.
    assertThat(profileData).isNotNull();

    Optional<ApplicantModel> maybeApplicant = oidcProfileCreator.getExistingApplicant(oidcProfile);
    assertThat(maybeApplicant).isPresent();
    assertThat(maybeApplicant.get().getPhoneNumber()).isEqualTo(Optional.empty());
  }

  @Test
  public void innerCreate_emptyOidcProfile_returnsEmpty() {
    CiviformOidcProfileCreator creator = getOidcProfileCreator();

    Optional<UserProfile> result =
        creator.innerCreate(/* profile= */ Optional.empty(), /* guestProfile= */ Optional.empty());

    assertThat(result).isEmpty();
  }

  @Test
  public void innerCreate_nonOidcProfile_returnsEmpty() {
    CiviformOidcProfileCreator creator = getOidcProfileCreator();
    BasicUserProfile basicProfile = new BasicUserProfile();
    basicProfile.setId("some-id");

    Optional<UserProfile> result =
        creator.innerCreate(Optional.of(basicProfile), /* guestProfile= */ Optional.empty());

    assertThat(result).isEmpty();
  }

  @Test
  public void innerCreate_newUser_noGuestProfile_createsNewProfile() {
    CiviformOidcProfileCreator creator = getOidcProfileCreator();

    Optional<UserProfile> result =
        creator.innerCreate(Optional.of(oidcProfile), /* guestProfile= */ Optional.empty());

    assertThat(result).isPresent();
    CiviFormProfileData profileData = (CiviFormProfileData) result.get();
    assertThat(profileData.getEmail()).isEqualTo(EMAIL);

    // Verify the applicant was persisted to the database.
    Optional<ApplicantModel> maybeApplicant = creator.getExistingApplicant(oidcProfile);
    assertThat(maybeApplicant).isPresent();
    assertThat(maybeApplicant.get().getAccount().getAuthorityId()).isEqualTo(AUTHORITY_ID);
  }

  @Test
  public void innerCreate_newUser_withGuestProfile_usesGuestProfile() {
    // Guest is logging in with no user in the database.  Guest data is retained supplemented by
    // OIDC data.
    CiviformOidcProfileCreator creator = getOidcProfileCreator();
    CiviFormProfileData guestProfileData = profileFactory.createNewApplicant();
    CiviFormProfile guestProfile = profileFactory.wrapProfileData(guestProfileData);

    Optional<UserProfile> result =
        creator.innerCreate(Optional.of(oidcProfile), Optional.of(guestProfile));

    assertThat(result).isPresent();
    CiviFormProfileData profileData = (CiviFormProfileData) result.get();
    assertThat(profileData.getEmail()).isEqualTo(EMAIL);
    Optional<ApplicantModel> maybeApplicant = creator.getExistingApplicant(oidcProfile);
    assertThat(maybeApplicant).isPresent();
    assertThat(maybeApplicant.get().getAccount().getAuthorityId()).isEqualTo(AUTHORITY_ID);
    // The guest profile's account should be reused since it's the first login.
    assertThat(profileData.getId()).isEqualTo(guestProfileData.getId());
  }

  @Test
  public void innerCreate_existingUser_noGuestProfile_returnsExistingAccount() {
    // Setup: create an existing account with the same authority ID.
    var existingAccount =
        resourceCreator
            .insertAccount()
            .setEmailAddress(EMAIL)
            .setAuthorityId(AUTHORITY_ID)
            .setApplicants(ImmutableList.of(resourceCreator.insertApplicant()));
    existingAccount.save();

    CiviformOidcProfileCreator creator = getOidcProfileCreator();

    Optional<UserProfile> result =
        creator.innerCreate(Optional.of(oidcProfile), /* guestProfile= */ Optional.empty());

    assertThat(result).isPresent();
    CiviFormProfileData profileData = (CiviFormProfileData) result.get();
    assertThat(profileData.getEmail()).isEqualTo(EMAIL);

    // Should use the existing account, not create a new one.
    assertThat(Long.parseLong(profileData.getId())).isEqualTo(existingAccount.id);
  }

  @Test
  public void innerCreate_existingUser_withGuestProfileNoApps_usesExistingAccount() {
    // Setup: existing account in DB.
    var existingAccount =
        resourceCreator
            .insertAccount()
            .setEmailAddress(EMAIL)
            .setAuthorityId(AUTHORITY_ID)
            .setApplicants(ImmutableList.of(resourceCreator.insertApplicant()));
    existingAccount.save();

    // Guest profile with no applications - should be discarded in favor of existing account.
    CiviFormProfileData guestProfileData = profileFactory.createNewApplicant();
    CiviFormProfile guestProfile = profileFactory.wrapProfileData(guestProfileData);

    CiviformOidcProfileCreator creator = getOidcProfileCreator();

    Optional<UserProfile> result =
        creator.innerCreate(Optional.of(oidcProfile), Optional.of(guestProfile));

    assertThat(result).isPresent();
    CiviFormProfileData profileData = (CiviFormProfileData) result.get();
    // Should use the existing account, not the guest.
    assertThat(Long.parseLong(profileData.getId())).isEqualTo(existingAccount.id);
  }

  @Test
  @Parameters({"true", "false"})
  public void innerCreate_existingTiUser_withGuestProfile_discardsGuestProfile(
      boolean guestHasApplications) {
    ApplicantModel tiApplicant = makeTrustedIntermediary();
    AccountModel tiAccount = tiApplicant.getAccount();
    // Connect the TI with the oidc profile.
    tiAccount.setEmailAddress(EMAIL).setAuthorityId(AUTHORITY_ID).save();

    // Create a guest profile (simulating a user who started as guest before TI login).
    CiviFormProfileData guestProfileData = profileFactory.createNewApplicant();
    CiviFormProfile guestProfile = profileFactory.wrapProfileData(guestProfileData);

    ApplicantModel guestApplicant = guestProfile.getApplicant().join();
    if (guestHasApplications) {
      resourceCreator.insertActiveApplication(
          guestApplicant, resourceCreator.insertActiveProgram("test-program"));
    }

    CiviformOidcProfileCreator creator = getOidcProfileCreator();

    Optional<UserProfile> result =
        creator.innerCreate(Optional.of(oidcProfile), Optional.of(guestProfile));

    assertThat(result).isPresent();
    CiviFormProfileData profileData = (CiviFormProfileData) result.get();
    // Should use the TI's existing account, not the guest.
    assertThat(Long.parseLong(profileData.getId())).isEqualTo(tiAccount.id);
    // The TI should not have the guest applicant merged into it.
    tiAccount.refresh();
    var tiApplicants = tiAccount.getApplicants();
    assertThat(tiApplicants).hasSize(1);
    assertThat(tiApplicants.get(0).getApplications()).hasSize(0);
    // Guest profile's account should remain separate (not merged).
    assertThat(guestProfileData.getId()).isNotEqualTo(profileData.getId());
    guestApplicant.refresh();
    assertThat(guestApplicant.getApplications()).hasSize(guestHasApplications ? 1 : 0);
  }

  @Test
  public void shouldDropGuestProfile_ti_returnsTrue() {
    ApplicantModel tiApplicant = makeTrustedIntermediary();
    AccountModel tiAccount = tiApplicant.getAccount();
    CiviFormProfile guestProfile =
        profileFactory.wrapProfileData(profileFactory.createNewApplicant());
    resourceCreator.insertActiveApplication(
        guestProfile.getApplicant().join(), resourceCreator.insertActiveProgram("test-program"));

    CiviformOidcProfileCreator creator = getOidcProfileCreator();

    assertThat(creator.shouldDropGuestProfile(tiAccount, guestProfile)).isTrue();
  }

  @Test
  public void shouldDropGuestProfile_programAdmin_returnsTrue() {
    AccountModel adminAccount = resourceCreator.insertAccount();
    adminAccount.addAdministeredProgram(
        resourceCreator.insertActiveProgram("administered-program").getProgramDefinition());
    adminAccount.save();
    CiviFormProfile guestProfile =
        profileFactory.wrapProfileData(profileFactory.createNewApplicant());
    resourceCreator.insertActiveApplication(
        guestProfile.getApplicant().join(), resourceCreator.insertActiveProgram("test-program"));

    CiviformOidcProfileCreator creator = getOidcProfileCreator();

    assertThat(creator.shouldDropGuestProfile(adminAccount, guestProfile)).isTrue();
  }

  @Test
  public void shouldDropGuestProfile_civiFormAdmin_returnsTrue() {
    AccountModel adminAccount = resourceCreator.insertAccount();
    adminAccount.setGlobalAdmin(true);
    adminAccount.save();
    CiviFormProfile guestProfile =
        profileFactory.wrapProfileData(profileFactory.createNewApplicant());
    resourceCreator.insertActiveApplication(
        guestProfile.getApplicant().join(), resourceCreator.insertActiveProgram("test-program"));

    CiviformOidcProfileCreator creator = getOidcProfileCreator();

    assertThat(creator.shouldDropGuestProfile(adminAccount, guestProfile)).isTrue();
  }

  @Test
  public void shouldDropGuestProfile_regularApplicant_returnsFalse() {
    AccountModel regularAccount = resourceCreator.insertAccount();
    CiviFormProfile guestProfile =
        profileFactory.wrapProfileData(profileFactory.createNewApplicant());
    resourceCreator.insertActiveApplication(
        guestProfile.getApplicant().join(), resourceCreator.insertActiveProgram("test-program"));

    CiviformOidcProfileCreator creator = getOidcProfileCreator();

    assertThat(creator.shouldDropGuestProfile(regularAccount, guestProfile)).isFalse();
  }

  /**
   * Returns an OidcProfile with required fields set. Uses the same constants as the main profile
   * created in setup() to ensure consistent authority IDs across tests.
   */
  private OidcProfile makeOidcProfile() {
    OidcProfile oidcProfile = new OidcProfile();
    oidcProfile.setId(SUBJECT);
    oidcProfile.addAttribute("iss", ISSUER);
    oidcProfile.addAttribute("user_emailid", EMAIL);
    return oidcProfile;
  }

  private ApplicantModel makeTrustedIntermediary() {
    AccountModel tiAccount = resourceCreator.insertAccount();
    tiAccount.setMemberOfGroup(tiGroup);
    ApplicantModel tiApplicant = resourceCreator.insertApplicant();
    tiApplicant.setAccount(tiAccount);
    tiApplicant.save();
    tiAccount.setApplicants(ImmutableList.of(tiApplicant));
    tiAccount.save();
    return tiApplicant;
  }
}
