package auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequest;

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
import java.util.concurrent.CompletableFuture;
import models.AccountModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.play.PlayWebContext;
import repository.AccountRepository;
import repository.ResetPostgres;
import services.applicant.ApplicantData;
import support.CfTestHelpers;

public class CiviformOidcProfileCreatorTest extends ResetPostgres {
  private static final String EMAIL = "foo@bar.com";
  private static final String NAME = "Philip J. Fry";
  private static final String ISSUER = "issuer";
  private static final String SUBJECT = "subject";
  private static final String AUTHORITY_ID = "iss: issuer sub: subject";
  private static final String ID_TOKEN_STRING = "id token string";

  private static OidcProfile profile;

  private ProfileFactory profileFactory;
  private static AccountRepository accountRepository;

  @Before
  public void setup() {
    accountRepository = instanceOf(AccountRepository.class);
    profileFactory = instanceOf(ProfileFactory.class);

    profile = new OidcProfile();
    profile.addAttribute("user_emailid", EMAIL);
    profile.addAttribute("user_displayname", NAME);
    profile.addAttribute("user_locale", "fr");
    profile.addAttribute("iss", ISSUER);
    profile.setId(SUBJECT);
    profile.setIdTokenString(ID_TOKEN_STRING);
  }

  private CiviformOidcProfileCreator getOidcProfileCreator(boolean enhancedLogoutEnabled) {
    Config civiformConfig =
        ConfigFactory.parseMap(
            ImmutableMap.of(
                "applicant_oidc_enhanced_logout_enabled", String.valueOf(enhancedLogoutEnabled)));
    OidcClient client = CfTestHelpers.getOidcClient("dev-oidc", 3390);
    OidcConfiguration oidcConfig = CfTestHelpers.getOidcConfiguration("dev-oidc", 3390);
    return new IdcsApplicantProfileCreator(
        oidcConfig,
        client,
        OidcClientProviderParams.create(
            civiformConfig,
            profileFactory,
            CfTestHelpers.userRepositoryProvider(accountRepository)));
  }

  private CiviformOidcProfileCreator getOidcProfileCreator() {
    return getOidcProfileCreator(false);
  }

  private CiviformOidcProfileCreator getOidcProfileCreatorWithEnhancedLogoutEnabled() {
    return getOidcProfileCreator(true);
  }

  @Test
  public void getExistingApplicant_succeeds_noAuthorityFallsBackToEmail() {
    // When an existing account doesn't have an authority_id we still find it by
    // email.

    // Setup.
    // Existing account doesn't have an authority.
    resourceCreator
        .insertAccount()
        .setEmailAddress(EMAIL)
        .setApplicants(ImmutableList.of(resourceCreator.insertApplicant()))
        .save();
    CiviformOidcProfileCreator oidcProfileAdapter = getOidcProfileCreator();

    // Execute.
    Optional<ApplicantModel> applicant = oidcProfileAdapter.getExistingApplicant(profile);

    // Verify.
    assertThat(applicant).isPresent();
    AccountModel account = applicant.get().getAccount();

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
    resourceCreator
        .insertAccount()
        .setEmailAddress(otherEmail)
        .setAuthorityId(AUTHORITY_ID)
        .setApplicants(ImmutableList.of(resourceCreator.insertApplicant()))
        .save();
    CiviformOidcProfileCreator oidcProfileAdapter = getOidcProfileCreator();

    // Execute.
    Optional<ApplicantModel> applicant = oidcProfileAdapter.getExistingApplicant(profile);

    // Verify.
    assertThat(applicant).isPresent();
    AccountModel account = applicant.get().getAccount();

    // The email of the existing account is the pre-existing one, not a new profile
    // one.
    assertThat(account.getEmailAddress()).isEqualTo(otherEmail);
    assertThat(account.getAuthorityId()).isEqualTo(AUTHORITY_ID);
  }

  @Test
  public void mergeCiviFormProfile_succeeds_new_user() {
    PlayWebContext context = new PlayWebContext(fakeRequest());
    CiviformOidcProfileCreator oidcProfileAdapter = getOidcProfileCreator();
    // Execute.
    CiviFormProfileData profileData =
        oidcProfileAdapter.mergeCiviFormProfile(Optional.empty(), profile, context);

    // Verify.
    assertThat(profileData).isNotNull();

    // The email of the existing account is the pre-existing one, not a new profile
    // one.
    assertThat(profileData.getEmail()).isEqualTo(EMAIL);

    Optional<ApplicantModel> maybeApplicant = oidcProfileAdapter.getExistingApplicant(profile);
    assertThat(maybeApplicant).isPresent();

    // The session ID is a random value, so just ensure it's set and not an empty string.
    assertThat(profileData.getSessionId()).isNotEmpty();

    ApplicantData applicantData = maybeApplicant.get().getApplicantData();

    assertThat(applicantData.getApplicantName().orElse("<empty optional>"))
        .isEqualTo("Fry, Philip");
    Locale l = applicantData.preferredLocale();
    assertThat(l).isEqualTo(Locale.FRENCH);
  }

  @Test
  public void mergeCiviFormProfile_existingUser_maintainsExistingData() {
    PlayWebContext context = new PlayWebContext(fakeRequest());
    CiviformOidcProfileCreator profileCreator = getOidcProfileCreator();
    OidcProfile oidcProfile = makeOidcProfile();
    CiviFormProfileData existingProfileData = profileFactory.createNewApplicant();
    CiviFormProfile existingProfile = profileFactory.wrapProfileData(existingProfileData);

    CiviFormProfileData mergedProfileData =
        profileCreator.mergeCiviFormProfile(Optional.of(existingProfile), oidcProfile, context);

    assertThat(existingProfileData.getSessionId()).isEqualTo(mergedProfileData.getSessionId());
    assertThat(existingProfileData.getId()).isEqualTo(mergedProfileData.getId());
  }

  @Test
  public void mergeCiviFormProfile_succeeds_new_user_with_enhanced_logout() {
    PlayWebContext context = new PlayWebContext(fakeRequest());
    CiviformOidcProfileCreator oidcProfileAdapter =
        getOidcProfileCreatorWithEnhancedLogoutEnabled();

    // Execute.
    CiviFormProfileData profileData =
        oidcProfileAdapter.mergeCiviFormProfile(Optional.empty(), profile, context);

    // Verify.
    assertThat(profileData).isNotNull();

    // The email of the existing account is the pre-existing one, not a new profile
    // one.
    assertThat(profileData.getEmail()).isEqualTo(EMAIL);

    Optional<ApplicantModel> maybeApplicant = oidcProfileAdapter.getExistingApplicant(profile);
    assertThat(maybeApplicant).isPresent();

    ApplicantData applicantData = maybeApplicant.get().getApplicantData();

    assertThat(applicantData.getApplicantName().orElse("<empty optional>"))
        .isEqualTo("Fry, Philip");
    Locale l = applicantData.preferredLocale();
    assertThat(l).isEqualTo(Locale.FRENCH);

    // Additional validations for enhanced logout behavior.
    AccountModel account = maybeApplicant.get().getAccount();
    SerializedIdTokens serializedIdTokens = account.getSerializedIdTokens();
    assertThat(serializedIdTokens.getIdToken(profileData.getSessionId())).hasValue(ID_TOKEN_STRING);
  }

  @Test
  public void mergeCiviFormProfile_skipped_forTrustedIntermediaries() {
    // Setup.
    AccountModel accountWithTiGroup = new AccountModel();
    accountWithTiGroup.setMemberOfGroup(new TrustedIntermediaryGroupModel("name", "description"));
    CiviFormProfile trustedIntermediary = mock(CiviFormProfile.class);
    when(trustedIntermediary.getAccount())
        .thenReturn(CompletableFuture.completedFuture(accountWithTiGroup));
    when(trustedIntermediary.getApplicant())
        .thenReturn(CompletableFuture.completedFuture(new ApplicantModel()));

    CiviFormProfileData fakeProfileData = new CiviFormProfileData(123L);
    when(trustedIntermediary.getProfileData()).thenReturn(fakeProfileData);

    PlayWebContext context = new PlayWebContext(fakeRequest());
    CiviformOidcProfileCreator oidcProfileAdapter = getOidcProfileCreator();

    // Execute.
    CiviFormProfileData profileData =
        oidcProfileAdapter.mergeCiviFormProfile(Optional.of(trustedIntermediary), profile, context);

    // Verify.
    // Profile data should still be present after the no-op merge.
    assertThat(profileData).isNotNull();
    assertThat(profileData).isEqualTo(fakeProfileData);

    // email is set
    assertThat(profileData.getEmail()).isEqualTo(EMAIL);
    assertThat(profileData.getDisplayName()).isNull();

    Optional<ApplicantModel> maybeApplicant = oidcProfileAdapter.getExistingApplicant(profile);
    assertThat(maybeApplicant).isNotPresent();
  }

  /** Returns an OidcProfile with required fields set. */
  private OidcProfile makeOidcProfile() {
    OidcProfile oidcProfile = new OidcProfile();
    oidcProfile.setId(SUBJECT);
    oidcProfile.addAttribute("iss", "my_issuer");
    oidcProfile.addAttribute("user_emailid", "foo@example.com");
    return oidcProfile;
  }
}
